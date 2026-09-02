package com.slmanju.ceylonads.promotion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the advanced promotion-slot model in isolation: each test creates its own admin slot +
 * plan with a small, deterministic capacity (rather than relying on the shared seed slots), so
 * capacity/overlap assertions never depend on how many promotions other tests have already
 * activated in the same shared test database.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class PromotionSlotTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void slotCapacityBlocksActivationOnceFull() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long slotId = createSlot(adminToken, "HOME_FEATURED", null, 1, 10);
        long planId = createPlan(adminToken, slotId, 7, "500.00");

        long adA = createApprovedAd(kamalToken, "Capacity Ad A " + UUID.randomUUID());
        long promoA = createPromotion(kamalToken, adA, planId);
        activate(adminToken, promoA, true);

        long adB = createApprovedAd(kamalToken, "Capacity Ad B " + UUID.randomUUID());
        long promoB = createPromotion(kamalToken, adB, planId);
        activate(adminToken, promoB, false);
    }

    @Test
    void availabilityEndpointReflectsRemainingCapacity() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long slotId = createSlot(adminToken, "HOME_FEATURED", null, 2, 10);
        long planId = createPlan(adminToken, slotId, 7, "500.00");

        mockMvc.perform(get("/api/promotion-slots/" + slotId + "/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.remainingCapacity").value(2));

        long adA = createApprovedAd(kamalToken, "Availability Ad A " + UUID.randomUUID());
        activate(adminToken, createPromotion(kamalToken, adA, planId), true);

        mockMvc.perform(get("/api/promotion-slots/" + slotId + "/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.remainingCapacity").value(1));

        long adB = createApprovedAd(kamalToken, "Availability Ad B " + UUID.randomUUID());
        activate(adminToken, createPromotion(kamalToken, adB, planId), true);

        mockMvc.perform(get("/api/promotion-slots/" + slotId + "/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.remainingCapacity").value(0));
    }

    @Test
    void categoryBoundSlotRejectsAdsFromOtherCategories() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long slotId = createSlot(adminToken, "CATEGORY_FEATURED", "vehicles", 4, 10);
        long planId = createPlan(adminToken, slotId, 7, "500.00");

        // "property" is a top-level category with no required attributes of its own (unlike
        // "mobile-phones", whose required attributes would make ad creation itself fail here).
        long mismatchedAd = createApprovedAdInCategory(kamalToken, "Mismatched Category Ad " + UUID.randomUUID(), "property");
        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", mismatchedAd, "promotionPlanId", planId))))
                .andExpect(status().isBadRequest());

        // A subcategory of the slot's bound category (cars is a child of vehicles) is compatible.
        long compatibleAd = createApprovedCar(kamalToken, "Compatible Category Ad " + UUID.randomUUID());
        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", compatibleAd, "promotionPlanId", planId))))
                .andExpect(status().isCreated());
    }

    @Test
    void inactiveSlotCannotBeActivatedInto() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long slotId = createSlot(adminToken, "HOME_FEATURED", null, 5, 10);
        long planId = createPlan(adminToken, slotId, 7, "500.00");

        long adId = createApprovedAd(kamalToken, "Inactive Slot Ad " + UUID.randomUUID());
        long promotionId = createPromotion(kamalToken, adId, planId);

        mockMvc.perform(patch("/api/admin/promotion-slots/" + slotId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        activate(adminToken, promotionId, false);
    }

    @Test
    @Transactional
    void expiredPromotionFreesCapacityForTheNextActivation() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long slotId = createSlot(adminToken, "HOME_FEATURED", null, 1, 10);
        long planId = createPlan(adminToken, slotId, 7, "500.00");

        long adA = createApprovedAd(kamalToken, "Expiry Frees Capacity A " + UUID.randomUUID());
        long promoA = createPromotion(kamalToken, adA, planId);
        activate(adminToken, promoA, true);

        long adB = createApprovedAd(kamalToken, "Expiry Frees Capacity B " + UUID.randomUUID());
        long promoB = createPromotion(kamalToken, adB, planId);
        activate(adminToken, promoB, false);

        // Simulate promoA's end date having already passed.
        entityManager.createQuery("update Promotion p set p.endsAt = :past where p.id = :id")
                .setParameter("past", Instant.now().minusSeconds(60))
                .setParameter("id", promoA)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        activate(adminToken, promoB, true);
    }

    @Test
    void paymentApprovalRespectsSlotCapacity() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long slotId = createSlot(adminToken, "HOME_FEATURED", null, 1, 10);
        long planId = createPlan(adminToken, slotId, 7, "500.00");

        long adA = createApprovedAd(kamalToken, "Payment Capacity Ad A " + UUID.randomUUID());
        long promoA = createPromotion(kamalToken, adA, planId);
        approveViaPayment(kamalToken, adminToken, promoA, true);

        long adB = createApprovedAd(kamalToken, "Payment Capacity Ad B " + UUID.randomUUID());
        long promoB = createPromotion(kamalToken, adB, planId);
        approveViaPayment(kamalToken, adminToken, promoB, false);
    }

    private void approveViaPayment(String customerToken, String adminToken, long promotionId, boolean expectSuccess) throws Exception {
        String response = mockMvc.perform(get("/api/payments/me").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long paymentId = 0;
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("promotionId").asLong() == promotionId) {
                paymentId = node.get("id").asLong();
            }
        }

        MockMultipartFile file = new MockMultipartFile("file", "receipt.png", "image/png", new byte[]{1, 2, 3, 4});
        mockMvc.perform(multipart("/api/payments/" + paymentId + "/receipt")
                        .file(file)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/payments/" + paymentId + "/submit")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("bankReference", "FT-" + UUID.randomUUID()))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(expectSuccess ? status().isOk() : status().isBadRequest());
    }

    private void activate(String adminToken, long promotionId, boolean expectSuccess) throws Exception {
        mockMvc.perform(patch("/api/admin/promotions/" + promotionId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(expectSuccess ? status().isOk() : status().isBadRequest());
    }

    private long createPromotion(String token, long adId, long planId) throws Exception {
        String response = mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createSlot(String adminToken, String placementType, String categorySlug, int capacity, int displayOrder) throws Exception {
        String code = "TEST_SLOT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("code", code);
        body.put("name", "Test Slot " + code);
        body.put("description", "A slot created for an isolated test scenario.");
        body.put("placementType", placementType);
        if (categorySlug != null) {
            body.put("categorySlug", categorySlug);
        }
        body.put("sourceChannel", "MAIN_SITE");
        body.put("capacity", capacity);
        body.put("displayOrder", displayOrder);

        String response = mockMvc.perform(post("/api/admin/promotion-slots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createPlan(String adminToken, long slotId, int durationDays, String price) throws Exception {
        String code = "TEST_PLAN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String response = mockMvc.perform(post("/api/admin/promotion-plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code,
                                "name", "Test Plan " + code,
                                "description", "A plan created for an isolated test scenario.",
                                "slotId", slotId,
                                "durationDays", durationDays,
                                "price", new BigDecimal(price)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createApprovedAd(String token, String title) throws Exception {
        return createApprovedAdInCategory(token, title, "vehicles");
    }

    private long createApprovedCar(String token, String title) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1000"),
                "categorySlug", "cars",
                "locationSlug", "colombo",
                "attributes", Map.of(
                        "make", "Toyota", "model", "Aqua", "year", "2020", "mileage", "50000",
                        "fuelType", "HYBRID", "transmission", "AUTOMATIC")));
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

    private long createApprovedAdInCategory(String token, String title, String categorySlug) throws Exception {
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
