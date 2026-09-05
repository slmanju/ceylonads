package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the Search Page Spotlight (TUITION_SEARCH_SIDEBAR_TOP / TUITION_SEARCH_SIDEBAR_TOP_30D)
 * capacity semantics after V25 raised capacity from 1 to 12: remaining capacity tracking as
 * promotions activate, the 13th concurrent activation being rejected, expiry freeing capacity back
 * up, and another Tuition slot never being counted against this one. Reuses the real seeded slot/
 * plan (not a synthetic admin-created slot like PromotionSlotTests) since the point is to prove the
 * actual Search Page Spotlight product behaves correctly, not the generic capacity mechanism in
 * isolation - that generic mechanism itself is already covered by PromotionSlotTests.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionSearchSpotlightCapacityTests {

    private static final String SPOTLIGHT_PLAN_CODE = "TUITION_SEARCH_SIDEBAR_TOP_30D";
    private static final String HOME_FEATURED_PLAN_CODE = "TUITION_HOME_FEATURED_30D";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void slotStartsAtCapacityTwelveWithNothingActive() throws Exception {
        assertAvailability(SPOTLIGHT_PLAN_CODE, 12, 12, true);
    }

    @Test
    @Transactional
    void remainingCapacityTracksActivationsUpToTwelveThenRejectsTheThirteenth() throws Exception {
        String customerToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = tuitionPlanIdByCode(SPOTLIGHT_PLAN_CODE);

        // 0 active -> 12 available
        assertAvailability(SPOTLIGHT_PLAN_CODE, 12, 12, true);

        // Activate 4 -> 8 remaining
        long[] firstFour = new long[4];
        for (int i = 0; i < 4; i++) {
            firstFour[i] = activateNewSpotlightPromotion(customerToken, adminToken, planId);
        }
        assertAvailability(SPOTLIGHT_PLAN_CODE, 12, 8, true);

        // Activate 7 more (11 total) -> 1 remaining
        for (int i = 0; i < 7; i++) {
            activateNewSpotlightPromotion(customerToken, adminToken, planId);
        }
        assertAvailability(SPOTLIGHT_PLAN_CODE, 12, 1, true);

        // Activate the 12th -> sold out
        activateNewSpotlightPromotion(customerToken, adminToken, planId);
        assertAvailability(SPOTLIGHT_PLAN_CODE, 12, 0, false);

        // The 13th concurrent request is still created successfully - FREE only zeroes the price,
        // it never bypasses admin approval, so creation itself never capacity-checks a pending
        // request (see PromotionService#resolveCreationPlan). Capacity is instead enforced
        // synchronously when an admin actually tries to approve it into the now-full slot.
        long thirteenthAdId = createApprovedTuitionAd(customerToken, "Spotlight Capacity Ad 13 " + UUID.randomUUID());
        String thirteenthResponse = mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", thirteenthAdId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        long thirteenthPromotionId = objectMapper.readTree(thirteenthResponse).get("id").asLong();
        approve(adminToken, thirteenthPromotionId, false);

        // Expiring one of the 12 active promotions frees capacity for a fresh purchase.
        entityManager.createQuery("update Promotion p set p.endsAt = :past where p.id = :id")
                .setParameter("past", Instant.now().minusSeconds(60))
                .setParameter("id", firstFour[0])
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        assertAvailability(SPOTLIGHT_PLAN_CODE, 12, 1, true);
        activateNewSpotlightPromotion(customerToken, adminToken, planId);
        assertAvailability(SPOTLIGHT_PLAN_CODE, 12, 0, false);
    }

    @Test
    void promotionInAnotherTuitionSlotDoesNotConsumeSpotlightCapacity() throws Exception {
        String customerToken = loginAndGetToken("kamal", "customer123");
        long homeFeaturedPlanId = tuitionPlanIdByCode(HOME_FEATURED_PLAN_CODE);

        // This promotion is only ever created, never activated - irrelevant to this test, since the
        // point is that a promotion on a *different* slot/plan never counts against Spotlight's own
        // capacity regardless of its own status.
        long adId = createApprovedTuitionAd(customerToken, "Home Featured Not Spotlight Ad " + UUID.randomUUID());
        promote(customerToken, adId, homeFeaturedPlanId);

        assertAvailability(SPOTLIGHT_PLAN_CODE, 12, 12, true);
    }

    // --- helpers --------------------------------------------------------------------------------

    // FREE only zeroes the price - a customer request still requires admin approval (see
    // PromotionService#resolveCreationPlan), so this plan (free via EZCLASS_LAUNCH_FREE) lands
    // PENDING_APPROVAL on creation; the actual capacity check runs when it's approved.
    private long activateNewSpotlightPromotion(String customerToken, String adminToken, long planId) throws Exception {
        long adId = createApprovedTuitionAd(customerToken, "Spotlight Capacity Ad " + UUID.randomUUID());
        String response = mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(response);
        long promotionId = created.get("id").asLong();
        if (!"ACTIVE".equals(created.get("status").asText())) {
            approve(adminToken, promotionId, true);
        }
        return promotionId;
    }

    private void assertAvailability(String planCode, int expectedSlotCapacity, int expectedRemaining, boolean expectedAvailable) throws Exception {
        mockMvc.perform(get("/api/tuition/promotions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.plan.code == '" + planCode + "')].plan.slotCapacity").value(expectedSlotCapacity))
                .andExpect(jsonPath("$[?(@.plan.code == '" + planCode + "')].remainingCapacity").value(expectedRemaining))
                .andExpect(jsonPath("$[?(@.plan.code == '" + planCode + "')].available").value(expectedAvailable));
    }

    private void approve(String adminToken, long promotionId, boolean expectSuccess) throws Exception {
        mockMvc.perform(patch("/api/admin/tuition/promotions/" + promotionId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(expectSuccess ? status().isOk() : status().isBadRequest());
    }

    private long promote(String token, long adId, long planId) throws Exception {
        String response = mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long tuitionPlanIdByCode(String code) throws Exception {
        String response = mockMvc.perform(get("/api/tuition/promotions/plans"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("plan").get("code").asText().equals(code)) {
                return node.get("plan").get("id").asLong();
            }
        }
        throw new IllegalStateException("Tuition plan not found: " + code);
    }

    private long createApprovedTuitionAd(String token, String title) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "A description long enough for validation purposes.");
        body.put("price", 3000);
        body.put("categorySlug", "school-tuition");
        body.put("locationSlugs", java.util.List.of("colombo"));
        body.put("subject", "Combined Mathematics");
        body.put("level", "A/L");
        body.put("curriculum", "LOCAL");
        body.put("medium", java.util.List.of("ENGLISH"));
        body.put("deliveryMode", "PHYSICAL");
        body.put("classFormat", "INDIVIDUAL");

        String response = mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        return id;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
