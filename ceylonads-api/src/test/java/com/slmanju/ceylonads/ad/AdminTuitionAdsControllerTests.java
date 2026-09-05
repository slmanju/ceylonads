package com.slmanju.ceylonads.ad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers /api/admin/tuition/** (see AdminTuitionAdsController): ADMIN-only, always scoped to
// SourceChannel.TUITION regardless of an id from another channel, reusing AdService's existing
// approve/reject/pendingReview/getForAdmin channel guard - never a duplicate moderation
// implementation.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdminTuitionAdsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private AdRepository ads;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void pendingListOnlyContainsTuitionClasses() throws Exception {
        String token = registerAndGetToken();
        long tuitionId = createTuitionClass(token, tuitionBody("Pending Tuition " + UUID.randomUUID()));
        long mainSiteId = createMainSiteAd(token, "Pending Main Site Ad " + UUID.randomUUID());

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(get("/api/admin/tuition/pending").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + tuitionId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + mainSiteId + ")]").doesNotExist());
    }

    @Test
    void approvingThroughTuitionAdminAppliesFreeListingExpiry() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Approve Via Tuition Admin " + UUID.randomUUID()));

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/tuition/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.publishedAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists());

        mockMvc.perform(get("/api/admin/tuition/pending").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").doesNotExist());
    }

    @Test
    void rejectingThroughTuitionAdminWorksAndOwnerStillSeesIt() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Reject Via Tuition Admin " + UUID.randomUUID()));

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/tuition/ads/" + id + "/reject").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/tuition/my-classes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
    }

    @Test
    void mainSiteAdCannotBeApprovedOrRejectedThroughTuitionAdmin() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long mainSiteAdId = createMainSiteAd(kamalToken, "Main Site Ad " + UUID.randomUUID());

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(get("/api/admin/tuition/ads/" + mainSiteAdId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/admin/tuition/ads/" + mainSiteAdId + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/admin/tuition/ads/" + mainSiteAdId + "/reject").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminCannotReachTuitionAdminEndpoints() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");

        mockMvc.perform(get("/api/admin/tuition/pending").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/tuition/pending").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/tuition/dashboard").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardReturnsChannelScopedCounts() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(get("/api/admin/tuition/dashboard").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingClasses").isNumber())
                .andExpect(jsonPath("$.activeClasses").isNumber())
                .andExpect(jsonPath("$.expiredClasses").isNumber())
                .andExpect(jsonPath("$.newSuggestions").isNumber())
                .andExpect(jsonPath("$.pendingPromotions").isNumber())
                .andExpect(jsonPath("$.activePromotions").isNumber())
                .andExpect(jsonPath("$.currentPromotionPlans").isNumber())
                .andExpect(jsonPath("$.currentCampaigns").isNumber());
    }

    // --- Admin "Promote Class" action (POST /api/admin/tuition/ads/{id}/promotions) ------------
    // See PromotionService#createAdminPromotionForTuitionClass: the admin's action is itself the
    // approval, so a successful call always lands ACTIVE, never PENDING_PAYMENT/PENDING_APPROVAL.

    @Test
    void promoteActivatesEligibleTuitionClassImmediately() throws Exception {
        String token = registerAndGetToken();
        long adId = createTuitionClass(token, tuitionBody("Promote Me " + UUID.randomUUID()));
        approveClassAsAdmin(adId);

        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_SEARCH_BOOST_30D");

        mockMvc.perform(post("/api/admin/tuition/ads/" + adId + "/promotions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", planId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.adId").value(adId))
                .andExpect(jsonPath("$.promotionPlanId").value(planId))
                .andExpect(jsonPath("$.durationDays").value(30))
                .andExpect(jsonPath("$.startsAt").exists())
                .andExpect(jsonPath("$.endsAt").exists());

        // Expiry protection: the class's expiresAt must now cover the 30-day promotion, even
        // though it was just approved (free listing) moments ago.
        var ad = ads.findById(adId).orElseThrow();
        assertTrue(ad.getExpiresAt() != null && !ad.getExpiresAt().isBefore(Instant.now().plus(29, ChronoUnit.DAYS)));
    }

    @Test
    void nonAdminCannotPromoteATuitionClass() throws Exception {
        String token = registerAndGetToken();
        long adId = createTuitionClass(token, tuitionBody("Not Promotable By Customer " + UUID.randomUUID()));
        approveClassAsAdmin(adId);

        mockMvc.perform(post("/api/admin/tuition/ads/" + adId + "/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    void mainSiteClassCannotBePromotedThroughTuitionAdmin() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long mainSiteAdId = createMainSiteAd(kamalToken, "Main Site Promote Attempt " + UUID.randomUUID());

        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_SEARCH_BOOST_30D");
        mockMvc.perform(post("/api/admin/tuition/ads/" + mainSiteAdId + "/promotions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", planId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void expiredTuitionClassCannotBePromoted() throws Exception {
        String token = registerAndGetToken();
        long adId = createTuitionClass(token, tuitionBody("Expired Class " + UUID.randomUUID()));
        approveClassAsAdmin(adId);

        var ad = ads.findById(adId).orElseThrow();
        ad.seedExpiryOverride(Instant.now().minus(1, ChronoUnit.DAYS), AdStatus.ACTIVE);
        ads.save(ad);

        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_SEARCH_BOOST_30D");
        mockMvc.perform(post("/api/admin/tuition/ads/" + adId + "/promotions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", planId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retiredTuitionPlanCannotBeUsedToPromote() throws Exception {
        String token = registerAndGetToken();
        long adId = createTuitionClass(token, tuitionBody("Retired Plan Attempt " + UUID.randomUUID()));
        approveClassAsAdmin(adId);

        String adminToken = loginAndGetToken("admin", "admin123");
        // TUITION_SEARCH_TOP_BANNER_7D sits on a slot (TUITION_SEARCH_TOP_BANNER) outside
        // TuitionPromotionCatalog.CURRENT_SLOT_CODES - retired, kept only for historical audit.
        long retiredPlanId = tuitionPlanIdByCode(adminToken, "TUITION_SEARCH_TOP_BANNER_7D");
        mockMvc.perform(post("/api/admin/tuition/ads/" + adId + "/promotions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", retiredPlanId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateActivePromotionForTheSamePlanIsRejectedButADifferentPlanIsAllowed() throws Exception {
        String token = registerAndGetToken();
        long adId = createTuitionClass(token, tuitionBody("Duplicate Promotion Check " + UUID.randomUUID()));
        approveClassAsAdmin(adId);

        String adminToken = loginAndGetToken("admin", "admin123");
        long boostPlanId = tuitionPlanIdByCode(adminToken, "TUITION_SEARCH_BOOST_30D");
        mockMvc.perform(post("/api/admin/tuition/ads/" + adId + "/promotions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", boostPlanId))))
                .andExpect(status().isOk());

        // Same class, same plan again: rejected as a duplicate active promotion for this placement.
        mockMvc.perform(post("/api/admin/tuition/ads/" + adId + "/promotions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", boostPlanId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already has")));

        // Same class, a different product: allowed to hold multiple simultaneous promotions.
        long homeFeaturedPlanId = tuitionPlanIdByCode(adminToken, "TUITION_HOME_FEATURED_30D");
        mockMvc.perform(post("/api/admin/tuition/ads/" + adId + "/promotions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", homeFeaturedPlanId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private void approveClassAsAdmin(long id) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/tuition/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private long tuitionPlanIdByCode(String adminToken, String code) throws Exception {
        String response = mockMvc.perform(get("/api/admin/tuition/promotion-plans")
                        .param("scope", "ALL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var plans = objectMapper.readTree(response);
        for (var entry : plans) {
            if (entry.get("code").asText().equals(code)) {
                return entry.get("id").asLong();
            }
        }
        throw new IllegalStateException(code + " plan not found");
    }

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

    private long createMainSiteAd(String token, String title) throws Exception {
        String body = """
                {"title":"%s","description":"A description long enough for validation.","price":1000,\
                "categorySlug":"vehicles","locationSlug":"colombo"}
                """.formatted(title);
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String registerAndGetToken() throws Exception {
        String username = "tuition_admin_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        Map<String, Object> body = Map.of(
                "username", username,
                "password", "customer123",
                "email", username + "@example.test",
                "displayName", "Admin Test Tutor");
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
