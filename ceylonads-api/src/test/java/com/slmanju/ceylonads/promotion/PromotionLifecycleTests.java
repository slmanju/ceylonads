package com.slmanju.ceylonads.promotion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class PromotionLifecycleTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void customerCanPromoteOwnActiveAd_andItStartsPendingPayment() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createApprovedAd(kamalToken, "Promotable Ad " + UUID.randomUUID(), "vehicles");
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");

        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.adId").value(adId))
                .andExpect(jsonPath("$.startsAt").doesNotExist())
                .andExpect(jsonPath("$.price").value(750.0));
    }

    @Test
    void customerCannotPromoteAnotherCustomersAd() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String nimalToken = loginAndGetToken("nimal", "customer123");
        long adId = createApprovedAd(kamalToken, "Kamal Only Ad " + UUID.randomUUID(), "vehicles");
        long planId = planIdByCode(nimalToken, "HOME_FEATURED_7D");

        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + nimalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonActiveAdCannotBePromoted() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");

        // Freshly created ads start out PENDING_REVIEW, not ACTIVE.
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content("""
                                {"title":"Pending Ad %s","description":"A description long enough for validation.","price":1000,"categorySlug":"vehicles","locationSlug":"colombo"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long pendingAdId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", pendingAdId, "promotionPlanId", planId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void planPriceIsSnapshottedOntoThePromotion() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Snapshot Price Ad " + UUID.randomUUID(), "vehicles");
        long planId = planIdByCode(kamalToken, "VEHICLES_FEATURED_7D");

        String createResponse = mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long promotionId = objectMapper.readTree(createResponse).get("id").asLong();
        BigDecimal originalPrice = new BigDecimal(objectMapper.readTree(createResponse).get("price").asText());

        // Admin raises the plan's price after the customer already agreed to the original amount.
        mockMvc.perform(put("/api/admin/promotion-plans/" + planId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Category Featured",
                                "description", "Appear above regular ads within your ad's category page.",
                                "price", new BigDecimal("1000.00"),
                                "durationDays", 7,
                                "active", true,
                                "displayOrder", 20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(1000.0));

        mockMvc.perform(get("/api/promotions/" + promotionId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(originalPrice.doubleValue()));
    }

    @Test
    void adminCanActivatePendingPromotion_andItSetsStartAndEndDates() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Activation Ad " + UUID.randomUUID(), "vehicles");
        long planId = planIdByCode(kamalToken, "TOP_SEARCH_7D");

        String createResponse = mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long promotionId = objectMapper.readTree(createResponse).get("id").asLong();

        // A customer cannot activate their own promotion; only admin can.
        mockMvc.perform(patch("/api/admin/promotions/" + promotionId + "/activate")
                        .header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/promotions/" + promotionId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startsAt").isNotEmpty())
                .andExpect(jsonPath("$.endsAt").isNotEmpty());
    }

    @Test
    void customerCanCancelOnlyWhilePending() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Cancel Ad " + UUID.randomUUID(), "vehicles");
        long planId = planIdByCode(kamalToken, "TOP_SEARCH_7D");

        String createResponse = mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long promotionId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(patch("/api/admin/promotions/" + promotionId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/promotions/" + promotionId + "/cancel")
                        .header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isBadRequest());
    }

    private long planIdByCode(String token, String code) throws Exception {
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

    private long createApprovedAd(String token, String title, String categorySlug) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1000"),
                "categorySlug", categorySlug,
                "locationSlug", "colombo"));
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
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
