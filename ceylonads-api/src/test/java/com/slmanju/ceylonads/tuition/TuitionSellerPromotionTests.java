package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers the Tuition UI's dedicated promotion namespace (GET /api/tuition/promotions/plans,
// POST /api/tuition/promotions, GET /api/tuition/promotions/my, GET /api/tuition/promotions/{id}):
// the catalog exposes only active TUITION-channel plans with backend-resolved pricing, creation is
// channel/ownership-scoped the same way TuitionMyClassPromotionTests already covers for the nested
// endpoint (both reuse PromotionService#createForTuitionAd), and "my"/"{id}" never leak a
// MAIN_SITE/BOARDING promotion belonging to the same customer.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionSellerPromotionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void catalogExposesOnlyActiveTuitionPlansWithPricingFields() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        String response = mockMvc.perform(get("/api/tuition/promotions/plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        java.util.Set<String> retiredOrGenericCodes = java.util.Set.of(
                "HOME_FEATURED_7D", "HOME_FEATURED_30D", "TOP_SEARCH_7D", "DETAIL_SIDEBAR_FEATURED",
                "VEHICLES_FEATURED_7D");

        boolean sawAnyPlan = false;
        for (JsonNode node : objectMapper.readTree(response)) {
            sawAnyPlan = true;
            JsonNode plan = node.get("plan");
            String code = plan.get("code").asText();
            assertFalse(retiredOrGenericCodes.contains(code), "non-Tuition plan leaked into catalog: " + code);
            assertTrue(plan.has("currentPrice"));
            assertTrue(plan.has("discounted"));
            assertTrue(node.has("available"));
            assertTrue(node.has("remainingCapacity"));
        }
        assertTrue(sawAnyPlan, "expected at least one seeded Tuition plan in the catalog");
    }

    @Test
    void plansCatalogIsPublic() throws Exception {
        mockMvc.perform(get("/api/tuition/promotions/plans")).andExpect(status().isOk());
    }

    @Test
    void ownerCanCreatePromotionViaDedicatedEndpoint() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Dedicated Endpoint Class " + UUID.randomUUID()));
        approveAsAdmin(id);
        long planId = tuitionPlanIdByCode(token, "TUITION_HOME_FEATURED_30D");

        mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", id, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adId").value(id))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void dedicatedEndpointRejectsMainSiteAdEvenWhenOwnedAndActive() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long mainSiteId = createApprovedMainSiteAd(token, "Main Site For Dedicated Promote " + UUID.randomUUID());
        long planId = tuitionPlanIdByCode(token, "TUITION_HOME_FEATURED_30D");

        mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", mainSiteId, "promotionPlanId", planId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void myEndpointExcludesMainSitePromotionsForSameCustomer() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        long tuitionId = createTuitionClass(token, tuitionBody("My Endpoint Tuition Class " + UUID.randomUUID()));
        approveAsAdmin(tuitionId);
        long tuitionPlanId = tuitionPlanIdByCode(token, "TUITION_HOME_FEATURED_30D");
        mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", tuitionId, "promotionPlanId", tuitionPlanId))))
                .andExpect(status().isCreated());

        long mainSiteId = createApprovedMainSiteAd(token, "My Endpoint Main Site Ad " + UUID.randomUUID());
        long genericPlanId = genericPlanIdByCode(token, "VEHICLES_FEATURED_7D");
        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", mainSiteId, "promotionPlanId", genericPlanId))))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(get("/api/tuition/promotions/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (JsonNode node : objectMapper.readTree(response)) {
            long adId = node.get("adId").asLong();
            assertFalse(adId == mainSiteId, "MAIN_SITE promotion leaked into Tuition 'my' list");
        }
        assertTrue(objectMapper.readTree(response).size() >= 1);
    }

    @Test
    void detailEndpointRejectsMainSitePromotionOwnedBySameCustomer() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long mainSiteId = createApprovedMainSiteAd(token, "Detail Endpoint Main Site Ad " + UUID.randomUUID());
        long genericPlanId = genericPlanIdByCode(token, "VEHICLES_FEATURED_7D");

        String response = mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", mainSiteId, "promotionPlanId", genericPlanId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long promotionId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/tuition/promotions/" + promotionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void detailEndpointReturnsOwnedTuitionPromotion() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Detail Endpoint Tuition Class " + UUID.randomUUID()));
        approveAsAdmin(id);
        long planId = tuitionPlanIdByCode(token, "TUITION_HOME_FEATURED_30D");

        String response = mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", id, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long promotionId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/tuition/promotions/" + promotionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adId").value(id));
    }

    // --- helpers --------------------------------------------------------------------------------

    private Map<String, Object> tuitionBody(String title) {
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

    private long createApprovedMainSiteAd(String token, String title) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1000"),
                "categorySlug", "vehicles",
                "locationSlug", "colombo"));
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();
        approveAsAdmin(id);
        return id;
    }

    private void approveAsAdmin(long id) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
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
