package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.ad.service.AdService;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers the Tuition-only expiry/renewal/15-listing-limit/promotion-protection policy (see
// CLAUDE.md "Tuition MVP business rules"). Every scenario registers its own fresh customer
// (see registerAndGetToken) rather than reusing the shared kamal/nimal seed fixtures, so the
// 15-listing-limit assertions here are never affected by however many tuition ads other test
// classes have already created for those shared fixture accounts in the same test run.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionAdExpiryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private AdRepository ads;

    @Autowired
    private AdService adService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    // --- LISTING LIFETIME -------------------------------------------------------------------

    @Test
    void newPendingTuitionListingHasNoPublishedOrExpiryTimestamps() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Pending Lifetime " + UUID.randomUUID()));

        Ad ad = ads.findById(id).orElseThrow();
        assertNull(ad.getPublishedAt());
        assertNull(ad.getExpiresAt());
    }

    @Test
    void approvalSetsPublishedAndExpiryToThirtyDays() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("First Approval " + UUID.randomUUID()));
        approveAsAdmin(id);

        Ad ad = ads.findById(id).orElseThrow();
        assertNotNull(ad.getPublishedAt());
        assertNotNull(ad.getExpiresAt());
        Duration lifetime = Duration.between(ad.getPublishedAt(), ad.getExpiresAt());
        assertTrue(Math.abs(lifetime.toDays() - 30) <= 1, "expected ~30 day free lifetime, was " + lifetime);
    }

    @Test
    void rejectionBeforePublicationLeavesNoExpiry() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Rejected Before Publish " + UUID.randomUUID()));

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/reject").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Ad ad = ads.findById(id).orElseThrow();
        assertEquals(AdStatus.REJECTED, ad.getStatus());
        assertNull(ad.getPublishedAt());
        assertNull(ad.getExpiresAt());
    }

    @Test
    void normalEditDoesNotResetExpiry() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Edit Preserves Expiry " + UUID.randomUUID()));
        approveAsAdmin(id);
        Instant originalExpiry = ads.findById(id).orElseThrow().getExpiresAt();

        mockMvc.perform(put("/api/tuition/classes/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(tuitionBody("Edit Preserves Expiry Updated " + UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        // Edit sends it back to moderation, but the free-listing clock must not have moved.
        assertEquals(originalExpiry, ads.findById(id).orElseThrow().getExpiresAt());

        approveAsAdmin(id);
        // Re-approval after an edit is not a fresh publication either.
        assertEquals(originalExpiry, ads.findById(id).orElseThrow().getExpiresAt());
    }

    // --- 15-LISTING LIMIT --------------------------------------------------------------------

    @Test
    void fifteenthConcurrentListingIsRejected() throws Exception {
        String token = registerAndGetToken();
        for (int i = 0; i < 15; i++) {
            createTuitionClass(token, tuitionBody("Limit Class " + i + " " + UUID.randomUUID()));
        }

        mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(tuitionBody("One Too Many " + UUID.randomUUID()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivatedListingDoesNotCountTowardTheLimit() throws Exception {
        String token = registerAndGetToken();
        long firstId = createTuitionClass(token, tuitionBody("Will Deactivate " + UUID.randomUUID()));
        for (int i = 0; i < 14; i++) {
            createTuitionClass(token, tuitionBody("Filler Class " + i + " " + UUID.randomUUID()));
        }
        // 15 total now; deactivating one frees a slot.
        mockMvc.perform(delete("/api/tuition/classes/" + firstId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(tuitionBody("Fits After Deactivation " + UUID.randomUUID()))))
                .andExpect(status().isCreated());
    }

    // --- PUBLIC VISIBILITY & SCHEDULED EXPIRY -------------------------------------------------

    @Test
    void activeListingPastExpiryIsHiddenBeforeSchedulerRuns() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Visibility Check " + UUID.randomUUID()));
        approveAsAdmin(id);
        forceExpiry(id, -1, AdStatus.ACTIVE);

        // Still ACTIVE in the DB (scheduler hasn't run yet), but the public read must already 404.
        assertEquals(AdStatus.ACTIVE, ads.findById(id).orElseThrow().getStatus());
        mockMvc.perform(get("/api/tuition/classes/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void schedulerExpiresOnlyOverdueTuitionAds() throws Exception {
        String token = registerAndGetToken();
        long overdueId = createTuitionClass(token, tuitionBody("Overdue Scheduler " + UUID.randomUUID()));
        approveAsAdmin(overdueId);
        forceExpiry(overdueId, -1, AdStatus.ACTIVE);

        long notYetId = createTuitionClass(token, tuitionBody("Not Yet Due Scheduler " + UUID.randomUUID()));
        approveAsAdmin(notYetId);

        adService.expireOverdue(SourceChannel.TUITION);

        assertEquals(AdStatus.EXPIRED, ads.findById(overdueId).orElseThrow().getStatus());
        assertEquals(AdStatus.ACTIVE, ads.findById(notYetId).orElseThrow().getStatus());
    }

    @Test
    void myClassesStillShowsAnExpiredListing() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Expired Still Owned " + UUID.randomUUID()));
        approveAsAdmin(id);
        forceExpiry(id, -1, AdStatus.EXPIRED);

        mockMvc.perform(get("/api/tuition/my-classes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].status").value("EXPIRED"));
    }

    // --- RENEWAL ------------------------------------------------------------------------------

    @Test
    void renewingAnExpiredListingReactivatesItForThirtyDays() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Renew From Expired " + UUID.randomUUID()));
        approveAsAdmin(id);
        forceExpiry(id, -2, AdStatus.EXPIRED);

        mockMvc.perform(post("/api/tuition/classes/" + id + "/renew").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Ad ad = ads.findById(id).orElseThrow();
        assertEquals(AdStatus.ACTIVE, ad.getStatus());
        long daysRemaining = Duration.between(Instant.now(), ad.getExpiresAt()).toDays();
        assertTrue(daysRemaining >= 28 && daysRemaining <= 30, "expected ~30 days remaining, was " + daysRemaining);
    }

    @Test
    void renewingAnActiveListingNearExpiryExtendsFromCurrentExpiry() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Renew Near Expiry " + UUID.randomUUID()));
        approveAsAdmin(id);
        forceExpiry(id, 3, AdStatus.ACTIVE);
        Instant beforeRenewal = ads.findById(id).orElseThrow().getExpiresAt();

        mockMvc.perform(post("/api/tuition/classes/" + id + "/renew").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Instant afterRenewal = ads.findById(id).orElseThrow().getExpiresAt();
        assertEquals(beforeRenewal.plus(Duration.ofDays(30)), afterRenewal);
    }

    @Test
    void renewingAnActiveListingFarFromExpiryIsRejected() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Renew Too Early " + UUID.randomUUID()));
        approveAsAdmin(id);
        forceExpiry(id, 20, AdStatus.ACTIVE);

        mockMvc.perform(post("/api/tuition/classes/" + id + "/renew").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherCustomerCannotRenewSomeoneElsesListing() throws Exception {
        String ownerToken = registerAndGetToken();
        long id = createTuitionClass(ownerToken, tuitionBody("Not Yours To Renew " + UUID.randomUUID()));
        approveAsAdmin(id);
        forceExpiry(id, -1, AdStatus.EXPIRED);

        String otherToken = registerAndGetToken();
        mockMvc.perform(post("/api/tuition/classes/" + id + "/renew").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // --- PROMOTION LIFETIME PROTECTION ---------------------------------------------------------

    @Test
    void activatingAPromotionExtendsAShorterListingExpiry() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Promotion Extends Short Expiry " + UUID.randomUUID()));
        approveAsAdmin(id);
        forceExpiry(id, 5, AdStatus.ACTIVE);

        Instant promotionEndsAt = purchaseAndActivateTuitionPromotion(token, id, "TUITION_HOME_FEATURED_30D");

        assertEquals(promotionEndsAt, ads.findById(id).orElseThrow().getExpiresAt());
    }

    @Test
    void activatingAPromotionDoesNotShortenALongerListingExpiry() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Promotion Does Not Shorten " + UUID.randomUUID()));
        approveAsAdmin(id);
        forceExpiry(id, 90, AdStatus.ACTIVE);
        Instant beforeExpiry = ads.findById(id).orElseThrow().getExpiresAt();

        purchaseAndActivateTuitionPromotion(token, id, "TUITION_HOME_FEATURED_30D");

        assertEquals(beforeExpiry, ads.findById(id).orElseThrow().getExpiresAt());
    }

    @Test
    void expiredListingCannotBePromotedUntilRenewed() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Cannot Promote Expired " + UUID.randomUUID()));
        approveAsAdmin(id);
        forceExpiry(id, -1, AdStatus.ACTIVE);

        long planId = compatiblePlanIdByCode(token, id, "TUITION_HOME_FEATURED_30D");
        mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", id, "promotionPlanId", planId))))
                .andExpect(status().isBadRequest());
    }

    // --- DEACTIVATION WITH ACTIVE PROMOTION -----------------------------------------------------

    @Test
    void deactivationIsBlockedWhileAPaidPromotionIsActive() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Blocked Deactivation " + UUID.randomUUID()));
        approveAsAdmin(id);

        purchaseAndActivateTuitionPromotion(token, id, "TUITION_HOME_FEATURED_30D");

        mockMvc.perform(delete("/api/tuition/classes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivationIsAllowedWithoutAnActivePromotion() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Allowed Deactivation " + UUID.randomUUID()));
        approveAsAdmin(id);

        mockMvc.perform(delete("/api/tuition/classes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // --- helpers --------------------------------------------------------------------------------

    private Map<String, Object> tuitionBody(String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "A description long enough for validation purposes.");
        body.put("price", 3000);
        body.put("categorySlug", "school-tuition");
        body.put("locationSlugs", List.of("colombo"));
        body.put("subject", "Combined Mathematics");
        body.put("level", "A/L");
        body.put("curriculum", "LOCAL");
        body.put("medium", List.of("ENGLISH"));
        body.put("deliveryMode", "PHYSICAL");
        body.put("classFormat", "INDIVIDUAL");
        return body;
    }

    private long createTuitionClass(String token, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void approveAsAdmin(long id) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // Deterministically overrides expiry state for a test scenario rather than waiting real days -
    // same seed-only Ad method the DEV sample data uses (see Ad#seedExpiryOverride).
    private void forceExpiry(long id, int daysFromNow, AdStatus status) {
        Ad ad = ads.findById(id).orElseThrow();
        ad.seedExpiryOverride(Instant.now().plus(Duration.ofDays(daysFromNow)), status);
        ads.save(ad);
    }

    // Purchases a promotion and returns its resulting endsAt once ACTIVE. Skips the separate admin
    // activation step when the purchase already auto-activated (a free-campaign plan - see
    // purchaseTuitionPromotion), otherwise falls back to activateAsAdmin exactly as before.
    private Instant purchaseAndActivateTuitionPromotion(String token, long adId, String planCode) throws Exception {
        long planId = compatiblePlanIdByCode(token, adId, planCode);
        String response = mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(response);
        long promotionId = created.get("id").asLong();
        String status = created.get("status").asText();
        if ("ACTIVE".equals(status)) {
            return Instant.parse(created.get("endsAt").asText());
        }
        // FREE only zeroes the price - a customer request still requires admin approval (see
        // PromotionService#resolveCreationPlan), so a free Tuition plan lands PENDING_APPROVAL, not
        // PENDING_PAYMENT; approve() is the Tuition-scoped equivalent of activateAsAdmin() below for
        // that path.
        if ("PENDING_APPROVAL".equals(status)) {
            return approveAsAdminPromotion(promotionId);
        }
        return activateAsAdmin(promotionId);
    }

    private Instant approveAsAdminPromotion(long promotionId) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String response = mockMvc.perform(patch("/api/admin/tuition/promotions/" + promotionId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Instant.parse(objectMapper.readTree(response).get("endsAt").asText());
    }

    private long compatiblePlanIdByCode(String token, long adId, String code) throws Exception {
        String response = mockMvc.perform(get("/api/tuition/my-classes/" + adId + "/promotion-plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("plan").get("code").asText().equals(code)) {
                return node.get("plan").get("id").asLong();
            }
        }
        throw new IllegalStateException("Compatible plan not found: " + code);
    }

    // Admin-activates a PENDING_PAYMENT/PENDING_APPROVAL promotion (standing in for the real bank
    // transfer approval step - see PromotionService#activate) and returns its resulting endsAt.
    private Instant activateAsAdmin(long promotionId) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String response = mockMvc.perform(patch("/api/admin/promotions/" + promotionId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Instant.parse(objectMapper.readTree(response).get("endsAt").asText());
    }

    private String registerAndGetToken() throws Exception {
        String username = "tuition_expiry_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        Map<String, Object> body = Map.of(
                "username", username,
                "password", "customer123",
                "email", username + "@example.test",
                "displayName", "Expiry Test Tutor");
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
