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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises GET /api/tuition/featured - the isolated, tuition-only counterpart to
 * /api/ads/category-featured (see TuitionFeaturedService). Mirrors the ad-creation/promote/
 * activate flow already used by PromotionRankingTests rather than relying on seeded data, so each
 * test controls exactly which promotions exist.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionFeaturedTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seed() throws Exception {
        seeder.run();
    }

    @Test
    void featuredIncludesAnActiveTuitionFeaturedPromotion() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        String title = "Featured Tuition Active " + UUID.randomUUID();

        long adId = createApprovedAd(kamalToken, title, "education-tuition");
        promoteAndActivate(kamalToken, adminToken, adId, "TUITION_HOME_FEATURED_30D");

        mockMvc.perform(get("/api/tuition/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")].title").value(title));
    }

    @Test
    void featuredExcludesPendingPromotions() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createApprovedAd(kamalToken, "Featured Tuition Pending " + UUID.randomUUID(), "education-tuition");
        long planId = planIdByCode(kamalToken, "TUITION_HOME_FEATURED_30D");

        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tuition/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")]").doesNotExist());
    }

    @Test
    void featuredExcludesAdsThatAreNoLongerActive() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Featured Tuition Deactivated " + UUID.randomUUID(), "education-tuition");
        promoteAndActivate(kamalToken, adminToken, adId, "TUITION_HOME_FEATURED_30D");

        mockMvc.perform(delete("/api/tuition/classes/" + adId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tuition/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")]").doesNotExist());
    }

    @Test
    @Transactional
    void featuredExcludesExpiredPromotions() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Featured Tuition Expired " + UUID.randomUUID(), "education-tuition");
        long promotionId = promoteAndActivate(kamalToken, adminToken, adId, "TUITION_HOME_FEATURED_30D");

        entityManager.createQuery("update Promotion p set p.endsAt = :past where p.id = :id")
                .setParameter("past", Instant.now().minusSeconds(60))
                .setParameter("id", promotionId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/tuition/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")]").doesNotExist());
    }

    @Test
    void featuredRespectsRequestedSize() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Featured Tuition Size " + UUID.randomUUID(), "education-tuition");
        promoteAndActivate(kamalToken, adminToken, adId, "TUITION_HOME_FEATURED_30D");

        String response = mockMvc.perform(get("/api/tuition/featured").param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(1, objectMapper.readTree(response).size());
    }

    @Test
    void featuredCardShapeIsLightweight() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Featured Tuition Shape " + UUID.randomUUID(), "education-tuition");
        promoteAndActivate(kamalToken, adminToken, adId, "TUITION_HOME_FEATURED_30D");

        String response = mockMvc.perform(get("/api/tuition/featured").param("size", "20"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode cards = objectMapper.readTree(response);

        JsonNode card = null;
        for (JsonNode node : cards) {
            if (node.get("id").asLong() == adId) {
                card = node;
            }
        }
        assertTrue(card != null, "expected the activated ad to be present in the featured list");

        assertTrue(card.has("id"));
        assertTrue(card.has("slug"));
        assertTrue(card.has("title"));
        assertTrue(card.has("price"));
        assertTrue(card.has("subject"));
        assertTrue(card.has("level"));
        assertTrue(card.has("curriculum"));
        assertTrue(card.has("medium"));
        assertTrue(card.has("deliveryMode"));
        assertTrue(card.has("primaryLocation"));
        assertTrue(card.has("primaryImageUrl"));
        assertTrue(card.has("providerName"));
        // Card DTO deliberately has no description/media-list/seller object/raw attribute list.
        assertFalse(card.has("description"));
        assertFalse(card.has("media"));
        assertFalse(card.has("seller"));
        assertFalse(card.has("attributes"));
    }

    private long promoteAndActivate(String customerToken, String adminToken, long adId, String planCode) throws Exception {
        long planId = planIdByCode(customerToken, planCode);
        String createResponse = mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long promotionId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(patch("/api/admin/promotions/" + promotionId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        return promotionId;
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

    // Uses the dedicated Tuition create API rather than generic /api/ads, so the resulting ad is
    // actually source_channel = TUITION - the featured carousel now requires that (see
    // TuitionPromotionRepository), not just the education-tuition category.
    private long createApprovedAd(String token, String title, String categorySlug) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1000"),
                "categorySlug", categorySlug,
                "locationSlugs", java.util.List.of("colombo")));
        String response = mockMvc.perform(post("/api/tuition/classes")
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
