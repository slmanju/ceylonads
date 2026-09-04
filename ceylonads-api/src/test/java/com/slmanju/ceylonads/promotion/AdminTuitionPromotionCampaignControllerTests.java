package com.slmanju.ceylonads.promotion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import com.slmanju.ceylonads.promotion.entity.PromotionCampaign;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import com.slmanju.ceylonads.promotion.repository.PromotionCampaignRepository;
import com.slmanju.ceylonads.promotion.repository.PromotionPlanRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers /api/admin/tuition/campaigns/** (see AdminTuitionPromotionCampaignController):
// ADMIN-only, always scoped to SourceChannel.TUITION, delegating entirely to
// PromotionCampaignService's existing create/update/setActive (overlap/pricing/date validation
// inherited, never reimplemented).
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdminTuitionPromotionCampaignControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private PromotionCampaignRepository campaignRepository;

    @Autowired
    private PromotionPlanRepository planRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void adminCanCreateEditActivateAndDeactivateATuitionCampaign() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = firstTuitionPlanId(adminToken);
        String code = "TEST_CAMPAIGN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Instant starts = Instant.now().plus(400, ChronoUnit.DAYS);
        Instant ends = starts.plus(10, ChronoUnit.DAYS);

        String createResponse = mockMvc.perform(post("/api/admin/tuition/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(campaignBody(code, "PERCENTAGE_DISCOUNT",
                                50, null, planId, starts, ends, false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        long campaignId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/admin/tuition/campaigns/" + campaignId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(campaignUpdateBody("Updated Name", "PERCENTAGE_DISCOUNT",
                                60, null, planId, starts, ends, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.discountPercent").value(60));

        mockMvc.perform(patch("/api/admin/tuition/campaigns/" + campaignId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/admin/tuition/campaigns/" + campaignId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        // Never deleted - remains visible in admin/history after deactivation.
        mockMvc.perform(get("/api/admin/tuition/campaigns").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + campaignId + ")]").exists());
    }

    @Test
    void hundredPercentDiscountCampaignDrivesCurrentPriceToZero() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = dedicatedTestPlanId(adminToken, "FREE_TEST");
        String code = "TEST_FREE_CAMPAIGN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Instant starts = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant ends = starts.plus(2, ChronoUnit.HOURS);

        mockMvc.perform(post("/api/admin/tuition/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(campaignBody(code, "PERCENTAGE_DISCOUNT",
                                100, null, planId, starts, ends, false))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/tuition/promotion-plans").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + planId + " && @.currentPrice == 0.0)]").exists());
    }

    @Test
    void overlappingCustomerVisibleCampaignOnSameChannelIsRejected() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = firstTuitionPlanId(adminToken);
        Instant starts = Instant.now().plus(500, ChronoUnit.DAYS);
        Instant ends = starts.plus(10, ChronoUnit.DAYS);

        String firstCode = "TEST_OVERLAP_A_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        mockMvc.perform(post("/api/admin/tuition/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(campaignBody(firstCode, "FIXED_PRICE",
                                null, java.math.BigDecimal.valueOf(500), planId, starts, ends, true))))
                .andExpect(status().isCreated());

        String secondCode = "TEST_OVERLAP_B_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        mockMvc.perform(post("/api/admin/tuition/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(campaignBody(secondCode, "FIXED_PRICE",
                                null, java.math.BigDecimal.valueOf(600), planId, starts.plus(5, ChronoUnit.DAYS),
                                ends.plus(5, ChronoUnit.DAYS), true))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editingAnActiveCampaignDoesNotRewriteAlreadyChargedPromotionPrices() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = dedicatedTestPlanId(adminToken, "SNAPSHOT_TEST");
        String code = "TEST_SNAPSHOT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Instant starts = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant ends = starts.plus(3, ChronoUnit.HOURS);

        mockMvc.perform(post("/api/admin/tuition/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(campaignBody(code, "PERCENTAGE_DISCOUNT",
                                50, null, planId, starts, ends, false))))
                .andExpect(status().isCreated());

        // Buy under the 50%-off campaign, capture the charged price.
        String token = registerAndGetToken();
        long adId = createTuitionClass(token, planId);
        approveClassAsAdmin(adId);
        String promotionResponse = mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var promotionNode = objectMapper.readTree(promotionResponse);
        long promotionId = promotionNode.get("id").asLong();
        double chargedPrice = promotionNode.get("price").asDouble();
        assertEquals(500.0, chargedPrice); // dedicatedTestPlanId sets base price 1000, 50% off = 500

        // Now edit the campaign to a different discount.
        String campaignsResponse = mockMvc.perform(get("/api/admin/tuition/campaigns").header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();
        long campaignId = 0;
        for (var c : objectMapper.readTree(campaignsResponse)) {
            if (c.get("code").asText().equals(code)) {
                campaignId = c.get("id").asLong();
            }
        }
        mockMvc.perform(put("/api/admin/tuition/campaigns/" + campaignId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(campaignUpdateBody("Snapshot Test", "PERCENTAGE_DISCOUNT",
                                20, null, planId, starts, ends, true, false))))
                .andExpect(status().isOk());

        // The already-created promotion's charged price must be untouched.
        mockMvc.perform(get("/api/admin/tuition/promotions/" + promotionId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(500.0));
    }

    @Test
    void campaignCreateRejectsARetiredNonCatalogPlan() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long legacyPlanId = legacyPlanId(adminToken, "TUITION_SEARCH_TOP_BANNER_7D");
        Instant starts = Instant.now().plus(600, ChronoUnit.DAYS);
        Instant ends = starts.plus(10, ChronoUnit.DAYS);

        mockMvc.perform(post("/api/admin/tuition/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(campaignBody(
                                "TEST_LEGACY_REJECT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                                "FIXED_PRICE", null, java.math.BigDecimal.valueOf(500), legacyPlanId, starts, ends, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void campaignUpdatePreservesAnAlreadyMappedHistoricalPlanButRejectsAddingANewOne() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long currentPlanId = firstTuitionPlanId(adminToken);
        long legacyPlanId = legacyPlanId(adminToken, "TUITION_SEARCH_TOP_BANNER_7D");
        long anotherLegacyPlanId = legacyPlanId(adminToken, "TUITION_FEATURED_7D");
        Instant starts = Instant.now().plus(700, ChronoUnit.DAYS);
        Instant ends = starts.plus(10, ChronoUnit.DAYS);

        String createResponse = mockMvc.perform(post("/api/admin/tuition/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(campaignBody(
                                "TEST_GRANDFATHER_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                                "FIXED_PRICE", null, java.math.BigDecimal.valueOf(500), currentPlanId, starts, ends, false))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long campaignId = objectMapper.readTree(createResponse).get("id").asLong();

        // Simulate a pre-existing historical mapping (e.g. from before this catalog restriction
        // existed) by attaching the legacy plan directly at the persistence layer.
        attachPlanDirectly(campaignId, legacyPlanId);

        // Updating without touching the legacy mapping (still included, unchanged) must succeed.
        Map<String, Object> preserveBody = campaignUpdateBody("Grandfather Test", "FIXED_PRICE", null,
                java.math.BigDecimal.valueOf(500), currentPlanId, starts, ends, true, false);
        preserveBody.put("planIds", List.of(currentPlanId, legacyPlanId));
        mockMvc.perform(put("/api/admin/tuition/campaigns/" + campaignId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(preserveBody)))
                .andExpect(status().isOk());

        // Adding a *different*, never-before-mapped historical plan must be rejected.
        Map<String, Object> addNewLegacyBody = campaignUpdateBody("Grandfather Test", "FIXED_PRICE", null,
                java.math.BigDecimal.valueOf(500), currentPlanId, starts, ends, true, false);
        addNewLegacyBody.put("planIds", List.of(currentPlanId, legacyPlanId, anotherLegacyPlanId));
        mockMvc.perform(put("/api/admin/tuition/campaigns/" + campaignId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(addNewLegacyBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ezclassLaunchFreeIsActiveAndEzclassLaunch990IsClosed() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String response = mockMvc.perform(get("/api/admin/tuition/campaigns").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Boolean freeActive = null;
        Boolean legacyActive = null;
        for (var c : objectMapper.readTree(response)) {
            if (c.get("code").asText().equals("EZCLASS_LAUNCH_FREE")) {
                freeActive = c.get("active").asBoolean();
            }
            if (c.get("code").asText().equals("EZCLASS_LAUNCH_990")) {
                legacyActive = c.get("active").asBoolean();
            }
        }
        assertTrue(Boolean.TRUE.equals(freeActive), "EZCLASS_LAUNCH_FREE should be the active current campaign");
        assertFalse(Boolean.TRUE.equals(legacyActive), "EZCLASS_LAUNCH_990 should be closed/historical, not presented as current");
    }

    private long legacyPlanId(String adminToken, String code) throws Exception {
        String response = mockMvc.perform(get("/api/admin/tuition/promotion-plans")
                        .param("scope", "ALL").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (var plan : objectMapper.readTree(response)) {
            if (plan.get("code").asText().equals(code)) {
                return plan.get("id").asLong();
            }
        }
        throw new IllegalStateException("Plan " + code + " not found");
    }

    private void attachPlanDirectly(long campaignId, long planId) {
        PromotionCampaign campaign = campaignRepository.findById(campaignId).orElseThrow();
        PromotionPlan plan = planRepository.findById(planId).orElseThrow();
        campaign.getPlans().add(plan);
        campaignRepository.save(campaign);
    }

    private Map<String, Object> campaignBody(
            String code, String pricingType, Integer discountPercent, java.math.BigDecimal fixedPrice,
            long planId, Instant starts, Instant ends, boolean customerVisible) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("name", "Test Campaign " + code);
        body.put("description", "A test campaign.");
        body.put("sourceChannel", "TUITION");
        body.put("pricingType", pricingType);
        if (discountPercent != null) body.put("discountPercent", discountPercent);
        if (fixedPrice != null) body.put("fixedPrice", fixedPrice);
        body.put("startsAt", starts.toString());
        body.put("endsAt", ends.toString());
        body.put("planIds", List.of(planId));
        body.put("customerVisible", customerVisible);
        body.put("showBanner", false);
        body.put("showModal", false);
        if (customerVisible) {
            body.put("headline", "Test headline");
            body.put("message", "Test message");
            body.put("ctaLabel", "Go");
        }
        return body;
    }

    private Map<String, Object> campaignUpdateBody(
            String name, String pricingType, Integer discountPercent, java.math.BigDecimal fixedPrice,
            long planId, Instant starts, Instant ends, boolean active, boolean customerVisible) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", "Updated description.");
        if (discountPercent != null) body.put("discountPercent", discountPercent);
        if (fixedPrice != null) body.put("fixedPrice", fixedPrice);
        body.put("startsAt", starts.toString());
        body.put("endsAt", ends.toString());
        body.put("active", active);
        body.put("planIds", List.of(planId));
        body.put("customerVisible", customerVisible);
        body.put("showBanner", false);
        body.put("showModal", false);
        if (customerVisible) {
            body.put("headline", "Test headline");
            body.put("message", "Test message");
            body.put("ctaLabel", "Go");
        }
        return body;
    }

    private long firstTuitionPlanId(String adminToken) throws Exception {
        String response = mockMvc.perform(get("/api/admin/tuition/promotion-plans").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var plans = objectMapper.readTree(response);
        for (var plan : plans) {
            if (plan.get("active").asBoolean()) {
                return plan.get("id").asLong();
            }
        }
        throw new IllegalStateException("No active TUITION promotion plan found");
    }

    // A dedicated freshly-created plan (base price 1000) isolated from EZCLASS_LAUNCH_FREE's own
    // plan mapping, so this test's campaign is the only one ever resolved for it - avoids
    // interference between the launch campaign and a test-created one on the same plan.
    private long dedicatedTestPlanId(String adminToken, String label) throws Exception {
        long slotId = firstTuitionSlotId(adminToken);
        String code = "TEST_DEDICATED_" + label + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String response = mockMvc.perform(post("/api/admin/tuition/promotion-plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code, "name", "Dedicated Test Plan", "description", "x",
                                "slotId", slotId, "durationDays", 30, "price", 1000))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long firstTuitionSlotId(String adminToken) throws Exception {
        String response = mockMvc.perform(get("/api/admin/tuition/promotion-plans/slots").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get(0).get("id").asLong();
    }

    private long createTuitionClass(String token, long planId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Campaign Snapshot Class " + UUID.randomUUID());
        body.put("description", "A description long enough for validation purposes.");
        body.put("price", 3000);
        body.put("categorySlug", "school-tuition");
        body.put("locationSlugs", List.of("colombo"));
        body.put("subject", "Combined Mathematics");
        body.put("level", "AL");
        body.put("curriculum", "LOCAL");
        body.put("medium", List.of("ENGLISH"));
        body.put("deliveryMode", "PHYSICAL");
        body.put("classFormat", "INDIVIDUAL");
        String response = mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void approveClassAsAdmin(long id) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String registerAndGetToken() throws Exception {
        String username = "tuition_campaign_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> body = Map.of(
                "username", username,
                "password", "customer123",
                "email", username + "@example.test",
                "displayName", "Campaign Test Tutor");
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
