package com.slmanju.ceylonads.promotion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import com.slmanju.ceylonads.promotion.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers /api/admin/tuition/promotions/** (see AdminTuitionPromotionController): ADMIN-only,
// always scoped to SourceChannel.TUITION via PromotionService's channel-scoped overloads - never
// a duplicate approve/reject/list implementation.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdminTuitionPromotionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private AdRepository ads;

    @Autowired
    private PromotionRepository promotions;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void adminCanListApproveAndSeeExpiryProtectionOnTuitionPromotion() throws Exception {
        String token = registerAndGetToken();
        long adId = createTuitionClass(token, tuitionBody("Promotion Review " + UUID.randomUUID()));
        approveClassAsAdmin(adId);
        long promotionId = createTuitionPromotion(token, adId);
        forcePendingApproval(promotionId);

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(get("/api/admin/tuition/promotions").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + promotionId + ")]").exists());

        mockMvc.perform(patch("/api/admin/tuition/promotions/" + promotionId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startsAt").exists())
                .andExpect(jsonPath("$.endsAt").exists());

        // The paid-duration guarantee: approving the promotion must extend the ad's expiresAt to
        // at least cover the promotion's own endsAt.
        Ad ad = ads.findById(adId).orElseThrow();
        assertTrue(ad.getExpiresAt() != null && !ad.getExpiresAt().isBefore(
                ad.getPublishedAt().plus(Duration.ofDays(29))));
    }

    @Test
    void adminCanRejectAPendingTuitionPromotionWithoutDeletingIt() throws Exception {
        String token = registerAndGetToken();
        long adId = createTuitionClass(token, tuitionBody("Promotion Reject " + UUID.randomUUID()));
        approveClassAsAdmin(adId);
        long promotionId = createTuitionPromotion(token, adId);
        forcePendingApproval(promotionId);

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/tuition/promotions/" + promotionId + "/reject")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Record kept, not deleted.
        mockMvc.perform(get("/api/admin/tuition/promotions/" + promotionId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // A customer-created Tuition promotion always lands PENDING_APPROVAL by the time this runs
    // (the ezClass launch campaign only zeroes the price, it never bypasses approval - see
    // PromotionService.resolveCreationPlan), but forcing the state directly here keeps these
    // approve/reject tests deterministic regardless of which campaigns/plan flags happen to be
    // configured when this test runs, the same way TuitionAdExpiryTests.forceExpiry forces expiry
    // state directly rather than waiting on real time.
    private void forcePendingApproval(long promotionId) {
        Promotion promotion = promotions.findById(promotionId).orElseThrow();
        promotion.seedLifecycleOverride(PromotionStatus.PENDING_APPROVAL, null, null);
        promotions.save(promotion);
    }

    @Test
    void nonAdminCannotReachTuitionPromotionAdminEndpoints() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        mockMvc.perform(get("/api/admin/tuition/promotions").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void mainSitePromotionCannotBeReachedThroughTuitionAdmin() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long mainSiteAdId = createMainSiteAd(kamalToken, "Main Site Promoted Ad " + UUID.randomUUID());
        approveClassAsAdmin(mainSiteAdId);
        long mainSitePromotionId = createMainSitePromotion(kamalToken, mainSiteAdId);

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(get("/api/admin/tuition/promotions/" + mainSitePromotionId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/admin/tuition/promotions/" + mainSitePromotionId + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/admin/tuition/promotions/" + mainSitePromotionId + "/reject").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/tuition/promotions").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + mainSitePromotionId + ")]").doesNotExist());
    }

    private long createTuitionPromotion(String token, long adId) throws Exception {
        long planId = tuitionSearchTopPlanId(token);
        String response = mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    // A customer-created promotion always lands PENDING_APPROVAL regardless of whether
    // EZCLASS_LAUNCH_FREE (100% off) happens to be active - FREE only zeroes the price, it never
    // bypasses approval (see PromotionService#resolveCreationPlan). Resolving the plan's
    // currently-compatible id via the API (rather than hardcoding one) just keeps this robust to
    // whichever Tuition plans/campaigns are configured when this test runs.
    private long tuitionSearchTopPlanId(String token) throws Exception {
        String response = mockMvc.perform(get("/api/tuition/promotions/plans").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var plans = objectMapper.readTree(response);
        for (var entry : plans) {
            if (entry.get("plan").get("code").asText().equals("TUITION_SEARCH_TOP_30D")) {
                return entry.get("plan").get("id").asLong();
            }
        }
        throw new IllegalStateException("TUITION_SEARCH_TOP_30D plan not found");
    }

    private long createMainSitePromotion(String token, long adId) throws Exception {
        long planId = topSearchPlanId(token);
        String response = mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long topSearchPlanId(String token) throws Exception {
        String plansResponse = mockMvc.perform(get("/api/promotion-plans").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var plans = objectMapper.readTree(plansResponse);
        for (var entry : plans) {
            if (entry.get("code").asText().equals("TOP_SEARCH_7D")) {
                return entry.get("id").asLong();
            }
        }
        throw new IllegalStateException("TOP_SEARCH_7D plan not found");
    }

    private Map<String, Object> tuitionBody(String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
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

    private void approveClassAsAdmin(long id) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String registerAndGetToken() throws Exception {
        String username = "tuition_promo_admin_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> body = Map.of(
                "username", username,
                "password", "customer123",
                "email", username + "@example.test",
                "displayName", "Promotion Test Tutor");
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
