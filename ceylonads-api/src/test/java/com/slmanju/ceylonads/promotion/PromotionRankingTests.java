package com.slmanju.ceylonads.promotion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class PromotionRankingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void homeFeaturedEndpointReturnsOnlyActiveHomeFeaturedAds() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");

        long activeAdId = createApprovedAd(kamalToken, "Home Featured Active " + UUID.randomUUID(), "vehicles");
        promoteAndActivate(kamalToken, adminToken, activeAdId, "HOME_FEATURED_7D");

        long pendingAdId = createApprovedAd(kamalToken, "Home Featured Pending " + UUID.randomUUID(), "vehicles");
        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", pendingAdId, "promotionPlanId", planId))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/ads/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + activeAdId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + activeAdId + ")].promoted").value(true))
                .andExpect(jsonPath("$[?(@.id == " + pendingAdId + ")]").doesNotExist());
    }

    @Test
    void categoryFeaturedAdRanksBeforeNewerNormalAdsInTheSameCategory() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        String marker = "CatFeatureMarker-" + UUID.randomUUID();

        long promotedAdId = createApprovedAd(kamalToken, marker + " promoted", "vehicles");
        promoteAndActivate(kamalToken, adminToken, promotedAdId, "VEHICLES_FEATURED_7D");

        Thread.sleep(5);
        long normalAdId = createApprovedAd(kamalToken, marker + " normal newer", "vehicles");

        mockMvc.perform(get("/api/ads").param("q", marker).param("category", "vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(promotedAdId))
                .andExpect(jsonPath("$.content[0].promoted").value(true))
                .andExpect(jsonPath("$.content[1].id").value(normalAdId))
                .andExpect(jsonPath("$.content[1].promoted").value(false))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void topSearchAdRanksBeforeNewerNormalAdsInGeneralBrowse() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        String marker = "TopSearchMarker-" + UUID.randomUUID();

        long promotedAdId = createApprovedAd(kamalToken, marker + " promoted", "vehicles");
        promoteAndActivate(kamalToken, adminToken, promotedAdId, "TOP_SEARCH_7D");

        Thread.sleep(5);
        long normalAdId = createApprovedAd(kamalToken, marker + " normal newer", "vehicles");

        mockMvc.perform(get("/api/ads").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(promotedAdId))
                .andExpect(jsonPath("$.content[1].id").value(normalAdId));
    }

    @Test
    void promotedAdThatDoesNotMatchTheQueryIsNotBoosted() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        String marker = "RelevanceMarker-" + UUID.randomUUID();

        long unrelatedPromotedAdId = createApprovedAd(kamalToken, "Unrelated Tuition " + UUID.randomUUID(), "education-tuition");
        promoteAndActivate(kamalToken, adminToken, unrelatedPromotedAdId, "TOP_SEARCH_7D");

        long matchingAdId = createApprovedAd(kamalToken, marker + " Toyota", "vehicles");

        mockMvc.perform(get("/api/ads").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matchingAdId));
    }

    @Test
    void filtersStillApplyToPromotedAds() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        String marker = "PriceFilterMarker-" + UUID.randomUUID();

        long expensivePromotedAdId = createApprovedAdWithPrice(kamalToken, marker + " expensive", "vehicles", new BigDecimal("500000"));
        promoteAndActivate(kamalToken, adminToken, expensivePromotedAdId, "TOP_SEARCH_7D");
        long cheapAdId = createApprovedAdWithPrice(kamalToken, marker + " cheap", "vehicles", new BigDecimal("1000"));

        mockMvc.perform(get("/api/ads").param("q", marker).param("maxPrice", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(cheapAdId));
    }

    @Test
    void cannotCreateDuplicatePendingOrActivePromotionForTheSamePlacement() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createApprovedAd(kamalToken, "Duplicate Guard Ad " + UUID.randomUUID(), "vehicles");
        long planId = planIdByCode(kamalToken, "TOP_SEARCH_7D");

        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void expiredPromotionNoLongerAffectsRanking() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        String marker = "ExpiryMarker-" + UUID.randomUUID();

        long expiredAdId = createApprovedAd(kamalToken, marker + " expired", "vehicles");
        long promotionId = promoteAndActivate(kamalToken, adminToken, expiredAdId, "TOP_SEARCH_7D");

        // Simulate the promotion's end date already having passed, without waiting real time.
        entityManager.createQuery("update Promotion p set p.endsAt = :past where p.id = :id")
                .setParameter("past", Instant.now().minusSeconds(60))
                .setParameter("id", promotionId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        Thread.sleep(5);
        long normalAdId = createApprovedAd(kamalToken, marker + " current", "vehicles");

        mockMvc.perform(get("/api/ads").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(normalAdId))
                .andExpect(jsonPath("$.content[0].promoted").value(false))
                .andExpect(jsonPath("$.content[1].id").value(expiredAdId))
                .andExpect(jsonPath("$.content[1].promoted").value(false));
    }

    @Test
    void categoryFeaturedPromotedAdThatFailsAttributeFilterIsExcluded() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        String marker = "AttrFilterPromoMarker-" + UUID.randomUUID();

        long promotedNonMatchingId = createApprovedCarWithFuelType(kamalToken, marker + " promoted petrol", "PETROL");
        promoteAndActivate(kamalToken, adminToken, promotedNonMatchingId, "VEHICLES_FEATURED_7D");

        long normalMatchingId = createApprovedCarWithFuelType(kamalToken, marker + " normal hybrid", "HYBRID");

        mockMvc.perform(get("/api/ads").param("q", marker).param("category", "cars").param("attr.fuelType", "HYBRID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(normalMatchingId))
                .andExpect(jsonPath("$.content[0].promoted").value(false));
    }

    @Test
    void categoryFeaturedPromotedAdThatMatchesAttributeFilterStillRanksFirst() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        String marker = "AttrFilterPromoMatchMarker-" + UUID.randomUUID();

        long promotedMatchingId = createApprovedCarWithFuelType(kamalToken, marker + " promoted hybrid", "HYBRID");
        promoteAndActivate(kamalToken, adminToken, promotedMatchingId, "VEHICLES_FEATURED_7D");

        Thread.sleep(5);
        long normalMatchingId = createApprovedCarWithFuelType(kamalToken, marker + " normal hybrid newer", "HYBRID");

        mockMvc.perform(get("/api/ads").param("q", marker).param("category", "cars").param("attr.fuelType", "HYBRID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(promotedMatchingId))
                .andExpect(jsonPath("$.content[0].promoted").value(true))
                .andExpect(jsonPath("$.content[1].id").value(normalMatchingId));
    }

    private long createApprovedCarWithFuelType(String token, String title, String fuelType) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1000"),
                "categorySlug", "cars",
                "locationSlug", "colombo",
                "attributes", Map.of(
                        "make", "Toyota", "model", "Aqua", "year", "2020", "mileage", "50000",
                        "fuelType", fuelType, "transmission", "AUTOMATIC")));

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

    private long createApprovedAd(String token, String title, String categorySlug) throws Exception {
        return createApprovedAdWithPrice(token, title, categorySlug, new BigDecimal("1000"));
    }

    private long createApprovedAdWithPrice(String token, String title, String categorySlug, BigDecimal price) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "A description long enough for validation.",
                "price", price,
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
