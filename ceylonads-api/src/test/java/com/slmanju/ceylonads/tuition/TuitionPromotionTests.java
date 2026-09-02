package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import com.slmanju.ceylonads.promotion.repository.PromotionSlotRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises GET /api/tuition/promotions - the Tuition search page's top-banner + 3-sidebar
 * placements (see TuitionPromotionService). Follows the same ad-creation/promote/activate flow as
 * TuitionFeaturedTests, against the 3 ad-backed sidebar slots (TUITION_SEARCH_SIDEBAR_TOP/MIDDLE/
 * BOTTOM); the banner slot (TUITION_SEARCH_TOP_BANNER) is admin/media-upload only to create and is
 * covered structurally (grouping, absence-when-empty) rather than by exercising a live banner.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionPromotionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PromotionSlotRepository promotionSlotRepository;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seed() throws Exception {
        seeder.run();
    }

    @Test
    void publishedTuitionAdPromotionIsReturnedInItsSidebarSlot() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        String title = "Sidebar Top Active " + UUID.randomUUID();

        long adId = createApprovedAd(kamalToken, title, "education-tuition");
        promoteAndActivate(kamalToken, adminToken, adId, "TUITION_SEARCH_SIDEBAR_TOP_7D");

        mockMvc.perform(get("/api/tuition/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sidebarTop[0].targetId").value(adId))
                .andExpect(jsonPath("$.sidebarTop[0].title").value(title))
                .andExpect(jsonPath("$.sidebarTop[0].slot").value("TUITION_SEARCH_SIDEBAR_TOP"))
                .andExpect(jsonPath("$.sidebarTop[0].type").value("AD"))
                .andExpect(jsonPath("$.sidebarMiddle").isEmpty())
                .andExpect(jsonPath("$.sidebarBottom").isEmpty())
                .andExpect(jsonPath("$.topBanner").isEmpty());
    }

    @Test
    void pendingPromotionsAreExcluded() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createApprovedAd(kamalToken, "Sidebar Top Pending " + UUID.randomUUID(), "education-tuition");
        long planId = planIdByCode(kamalToken, "TUITION_SEARCH_SIDEBAR_TOP_7D");

        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tuition/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sidebarTop[?(@.targetId == " + adId + ")]").doesNotExist());
    }

    @Test
    void deactivatedAdsAreExcluded() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Sidebar Top Deactivated " + UUID.randomUUID(), "education-tuition");
        promoteAndActivate(kamalToken, adminToken, adId, "TUITION_SEARCH_SIDEBAR_TOP_7D");

        mockMvc.perform(delete("/api/ads/" + adId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tuition/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sidebarTop[?(@.targetId == " + adId + ")]").doesNotExist());
    }

    @Test
    @Transactional
    void expiredPromotionsAreExcluded() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Sidebar Top Expired " + UUID.randomUUID(), "education-tuition");
        long promotionId = promoteAndActivate(kamalToken, adminToken, adId, "TUITION_SEARCH_SIDEBAR_TOP_7D");

        entityManager.createQuery("update Promotion p set p.endsAt = :past where p.id = :id")
                .setParameter("past", Instant.now().minusSeconds(60))
                .setParameter("id", promotionId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/tuition/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sidebarTop[?(@.targetId == " + adId + ")]").doesNotExist());
    }

    @Test
    void inactiveSlotReturnsNoPromotionsForThatSlot() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Sidebar Middle Inactive Slot " + UUID.randomUUID(), "education-tuition");
        promoteAndActivate(kamalToken, adminToken, adId, "TUITION_SEARCH_SIDEBAR_MIDDLE_7D");

        long slotId = promotionSlotRepository.findByCode("TUITION_SEARCH_SIDEBAR_MIDDLE").orElseThrow().getId();
        mockMvc.perform(patch("/api/admin/promotion-slots/" + slotId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tuition/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sidebarMiddle[?(@.targetId == " + adId + ")]").doesNotExist());
    }

    @Test
    void adsOutsideTheTuitionCategoryCannotBePromotedIntoATuitionSlot() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long planId = planIdByCode(kamalToken, "TUITION_SEARCH_SIDEBAR_BOTTOM_7D");
        long nonTuitionAdId = createApprovedAd(kamalToken, "Non Tuition Ad " + UUID.randomUUID(), "vehicles");

        mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", nonTuitionAdId, "promotionPlanId", planId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void multipleRequestedSlotsAreReturnedInOneCall() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long topAdId = createApprovedAd(kamalToken, "Multi Slot Top " + UUID.randomUUID(), "education-tuition");
        promoteAndActivate(kamalToken, adminToken, topAdId, "TUITION_SEARCH_SIDEBAR_TOP_7D");
        long middleAdId = createApprovedAd(kamalToken, "Multi Slot Middle " + UUID.randomUUID(), "education-tuition");
        promoteAndActivate(kamalToken, adminToken, middleAdId, "TUITION_SEARCH_SIDEBAR_MIDDLE_7D");

        mockMvc.perform(get("/api/tuition/promotions")
                        .param("slots", "TUITION_SEARCH_SIDEBAR_TOP,TUITION_SEARCH_SIDEBAR_MIDDLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sidebarTop[?(@.targetId == " + topAdId + ")]").exists())
                .andExpect(jsonPath("$.sidebarMiddle[?(@.targetId == " + middleAdId + ")]").exists())
                .andExpect(jsonPath("$.sidebarBottom").isEmpty());
    }

    @Test
    void responseShapeIsLightweight() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long adId = createApprovedAd(kamalToken, "Shape Check " + UUID.randomUUID(), "education-tuition");
        promoteAndActivate(kamalToken, adminToken, adId, "TUITION_SEARCH_SIDEBAR_TOP_7D");

        String response = mockMvc.perform(get("/api/tuition/promotions"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode card = objectMapper.readTree(response).get("sidebarTop").get(0);

        Set<String> expectedFields = Set.of(
                "id", "slot", "type", "title", "subtitle", "imageUrl", "badge", "ctaLabel",
                "targetUrl", "targetType", "targetId", "adSlug", "displayOrder");
        card.fieldNames().forEachRemaining(field -> assertTrue(expectedFields.contains(field), "unexpected field: " + field));
        assertEquals("AD", card.get("type").asText());
        assertEquals("SPONSORED", card.get("badge").asText());
        assertEquals("View Class", card.get("ctaLabel").asText());
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
