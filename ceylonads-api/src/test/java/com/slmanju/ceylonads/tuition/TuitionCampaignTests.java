package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import com.slmanju.ceylonads.promotion.repository.PromotionCampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers GET /api/tuition/promotions/campaign: the storefront banner/modal presentation endpoint,
// resolved by PromotionCampaignService#findActiveCustomerCampaign. V27 (ezClass free launch) seeds
// EZCLASS_LAUNCH_FREE customer_visible=true AND active=true covering "now" - the real live launch
// offer - so unlike the old (deactivated) EZCLASS_LAUNCH_990/EZCLASS_HALF_PRICE scaffolding this
// class was originally written against, "no active campaign" is no longer the default state.
// Every test here is @Transactional and deactivates EZCLASS_LAUNCH_FREE first (rolled back
// automatically afterwards) to restore the clean baseline these generic campaign-mechanics tests
// are actually about, then activates its own throwaway campaign via the admin API as before.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionCampaignTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private PromotionCampaignRepository promotionCampaignRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    // Deactivates the real EZCLASS_LAUNCH_FREE launch campaign (live-by-default since V27) via the
    // admin API so each test starts from a clean "no active TUITION campaign" slate, exactly as
    // this class originally assumed. Must be called from a @Transactional test so the deactivation
    // rolls back afterwards instead of leaking into other test classes sharing this H2 instance.
    private void deactivateDefaultLaunchCampaign(String adminToken) throws Exception {
        long id = promotionCampaignRepository.findByCode("EZCLASS_LAUNCH_FREE").orElseThrow().getId();
        mockMvc.perform(patch("/api/admin/promotion-campaigns/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void noActiveCampaignReturnsNoContent() throws Exception {
        deactivateDefaultLaunchCampaign(loginAndGetToken("admin", "admin123"));
        mockMvc.perform(get("/api/tuition/promotions/campaign")).andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void activeCustomerVisibleTuitionCampaignIsReturnedWithPresentationFields() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        deactivateDefaultLaunchCampaign(adminToken);
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_HOME_FEATURED_30D");
        Instant now = Instant.now();
        createCampaign(adminToken, "ACTIVE_" + UUID.randomUUID(), "TUITION", planId,
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                true, true, true, "Promote any Tuition Ad for just Rs. 990",
                "Flat rate for all eligible promotion placements.", "Promote My Ad");

        mockMvc.perform(get("/api/tuition/promotions/campaign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Promote any Tuition Ad for just Rs. 990"))
                .andExpect(jsonPath("$.message").value("Flat rate for all eligible promotion placements."))
                .andExpect(jsonPath("$.ctaLabel").value("Promote My Ad"))
                .andExpect(jsonPath("$.showBanner").value(true))
                .andExpect(jsonPath("$.showModal").value(true));
    }

    @Test
    @Transactional
    void showBannerAndShowModalAreReturnedIndependently() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        deactivateDefaultLaunchCampaign(adminToken);
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_HOME_FEATURED_30D");
        Instant now = Instant.now();
        createCampaign(adminToken, "BANNER_ONLY_" + UUID.randomUUID(), "TUITION", planId,
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                true, true, false, "Headline", "Message", "CTA");

        mockMvc.perform(get("/api/tuition/promotions/campaign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showBanner").value(true))
                .andExpect(jsonPath("$.showModal").value(false));
    }

    @Test
    @Transactional
    void futureCampaignIsNotReturned() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        deactivateDefaultLaunchCampaign(adminToken);
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_HOME_FEATURED_30D");
        Instant now = Instant.now();
        createCampaign(adminToken, "FUTURE_" + UUID.randomUUID(), "TUITION", planId,
                now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS),
                true, true, true, "Headline", "Message", "CTA");

        mockMvc.perform(get("/api/tuition/promotions/campaign")).andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void expiredCampaignIsNotReturned() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        deactivateDefaultLaunchCampaign(adminToken);
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_HOME_FEATURED_30D");
        Instant now = Instant.now();
        createCampaign(adminToken, "EXPIRED_" + UUID.randomUUID(), "TUITION", planId,
                now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS),
                true, true, true, "Headline", "Message", "CTA");

        mockMvc.perform(get("/api/tuition/promotions/campaign")).andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void deactivatedCampaignIsNotReturned() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        deactivateDefaultLaunchCampaign(adminToken);
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_HOME_FEATURED_30D");
        Instant now = Instant.now();
        long campaignId = createCampaign(adminToken, "INACTIVE_" + UUID.randomUUID(), "TUITION", planId,
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                true, true, true, "Headline", "Message", "CTA");

        mockMvc.perform(patch("/api/admin/promotion-campaigns/" + campaignId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tuition/promotions/campaign")).andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void notCustomerVisibleCampaignIsNotReturned() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        deactivateDefaultLaunchCampaign(adminToken);
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_HOME_FEATURED_30D");
        Instant now = Instant.now();
        createCampaign(adminToken, "NOT_VISIBLE_" + UUID.randomUUID(), "TUITION", planId,
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                false, false, false, null, null, null);

        mockMvc.perform(get("/api/tuition/promotions/campaign")).andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void mainSiteCampaignIsNotReturnedFromTuitionEndpoint() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        deactivateDefaultLaunchCampaign(adminToken);
        long planId = genericPlanIdByCode(adminToken, "VEHICLES_FEATURED_7D");
        Instant now = Instant.now();
        createCampaign(adminToken, "MAIN_SITE_" + UUID.randomUUID(), "MAIN_SITE", planId,
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                true, true, true, "Headline", "Message", "CTA");

        mockMvc.perform(get("/api/tuition/promotions/campaign")).andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void overlappingActiveCustomerVisibleCampaignsAreRejectedAtCreation() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        deactivateDefaultLaunchCampaign(adminToken);
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_HOME_FEATURED_30D");
        Instant now = Instant.now();
        createCampaign(adminToken, "FIRST_" + UUID.randomUUID(), "TUITION", planId,
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                true, true, true, "Headline", "Message", "CTA");

        Map<String, Object> overlapping = campaignRequestBody(
                "SECOND_" + UUID.randomUUID(), "TUITION", planId,
                now.minus(30, ChronoUnit.MINUTES), now.plus(2, ChronoUnit.HOURS),
                true, true, true, "Headline 2", "Message 2", "CTA 2");
        mockMvc.perform(post("/api/admin/promotion-campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(overlapping)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerVisibleCampaignRequiresNonBlankPresentationFields() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_HOME_FEATURED_30D");
        Instant now = Instant.now();

        Map<String, Object> missingHeadline = campaignRequestBody(
                "MISSING_HEADLINE_" + UUID.randomUUID(), "TUITION", planId,
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                true, true, true, null, "Message", "CTA");
        mockMvc.perform(post("/api/admin/promotion-campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(missingHeadline)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void showBannerRequiresCustomerVisible() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = tuitionPlanIdByCode(adminToken, "TUITION_HOME_FEATURED_30D");
        Instant now = Instant.now();

        Map<String, Object> invalid = campaignRequestBody(
                "SHOW_BANNER_NO_VISIBLE_" + UUID.randomUUID(), "TUITION", planId,
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                false, true, false, null, null, null);
        mockMvc.perform(post("/api/admin/promotion-campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // --- helpers --------------------------------------------------------------------------------

    private Map<String, Object> campaignRequestBody(
            String code, String sourceChannel, long planId, Instant startsAt, Instant endsAt,
            boolean customerVisible, boolean showBanner, boolean showModal,
            String headline, String message, String ctaLabel) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", code);
        request.put("name", "Test Campaign " + code);
        request.put("description", "Test-only campaign.");
        request.put("sourceChannel", sourceChannel);
        request.put("pricingType", "FIXED_PRICE");
        request.put("fixedPrice", "990.00");
        request.put("startsAt", startsAt.toString());
        request.put("endsAt", endsAt.toString());
        request.put("planIds", List.of(planId));
        request.put("customerVisible", customerVisible);
        request.put("showBanner", showBanner);
        request.put("showModal", showModal);
        if (headline != null) request.put("headline", headline);
        if (message != null) request.put("message", message);
        if (ctaLabel != null) request.put("ctaLabel", ctaLabel);
        return request;
    }

    private long createCampaign(
            String adminToken, String code, String sourceChannel, long planId, Instant startsAt, Instant endsAt,
            boolean customerVisible, boolean showBanner, boolean showModal,
            String headline, String message, String ctaLabel) throws Exception {
        Map<String, Object> request = campaignRequestBody(
                code, sourceChannel, planId, startsAt, endsAt, customerVisible, showBanner, showModal,
                headline, message, ctaLabel);
        String response = mockMvc.perform(post("/api/admin/promotion-campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long tuitionPlanIdByCode(String token, String code) throws Exception {
        String response = mockMvc.perform(get("/api/tuition/promotions/plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("plan").get("code").asText().equals(code)) {
                return node.get("plan").get("id").asLong();
            }
        }
        throw new IllegalStateException("Tuition plan not found: " + code);
    }

    private long genericPlanIdByCode(String token, String code) throws Exception {
        String response = mockMvc.perform(get("/api/promotion-plans").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("code").asText().equals(code)) {
                return node.get("id").asLong();
            }
        }
        throw new IllegalStateException("Seed plan not found: " + code);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
