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

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises GET /api/tuition/promotions/plans - the Tuition catalog restored to seven products by
 * V22 (Search Page Spotlight back on TUITION_SEARCH_SIDEBAR_TOP_30D, alongside the six V18 kept).
 * Base-price math itself (launch/50%-floor/no-campaign) is already covered generically by
 * PromotionPricingServiceTest; what this class proves is that this specific plan is actually wired
 * into the live catalog at the right price and participates in both campaigns like the other six.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionPromotionCatalogTests {

    private static final String SPOTLIGHT_PLAN_CODE = "TUITION_SEARCH_SIDEBAR_TOP_30D";
    private static final String SPOTLIGHT_SLOT_CODE = "TUITION_SEARCH_SIDEBAR_TOP";

    private static final Set<String> EXPECTED_PLAN_CODES = Set.of(
            "TUITION_SEARCH_TOP_30D", "TUITION_SEARCH_BOOST_30D", SPOTLIGHT_PLAN_CODE,
            "TUITION_HOME_FEATURED_30D", "TUITION_DETAIL_TOP_30D", "TUITION_HOME_LATEST_RIGHT_30D", "TUITION_DETAIL_RIGHT_30D");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromotionCampaignRepository promotionCampaignRepository;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seed() throws Exception {
        seeder.run();
    }

    // The real EZCLASS_LAUNCH_FREE launch campaign (live by default since V27) already covers all
    // seven Tuition plans, so tests exercising base pricing or the older EZCLASS_LAUNCH_990/
    // EZCLASS_HALF_PRICE campaigns must deactivate it first - rolled back automatically since every
    // caller is @Transactional.
    private void deactivateDefaultLaunchCampaign() throws Exception {
        long id = promotionCampaignRepository.findByCode("EZCLASS_LAUNCH_FREE").orElseThrow().getId();
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/promotion-campaigns/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void catalogContainsExactlySevenActiveTuitionProducts() throws Exception {
        String response = mockMvc.perform(get("/api/tuition/promotions/plans"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode plans = objectMapper.readTree(response);

        assertEquals(7, plans.size(), "expected exactly seven active Tuition products: " + response);

        Set<String> actualCodes = new java.util.HashSet<>();
        plans.forEach(node -> actualCodes.add(node.get("plan").get("code").asText()));
        assertEquals(EXPECTED_PLAN_CODES, actualCodes);
    }

    @Test
    void catalogNeverIncludesRetiredSearchSidebarMiddleOrBottom() throws Exception {
        mockMvc.perform(get("/api/tuition/promotions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.plan.code == 'TUITION_SEARCH_SIDEBAR_MIDDLE_7D')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.plan.code == 'TUITION_SEARCH_SIDEBAR_BOTTOM_7D')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.plan.code == 'TUITION_SEARCH_SIDEBAR_TOP_7D')]").doesNotExist());
    }

    @Test
    @Transactional
    void searchPageSpotlightHasFriendlyNameAndCorrectBasePriceWithNoCampaignActive() throws Exception {
        deactivateDefaultLaunchCampaign();

        mockMvc.perform(get("/api/tuition/promotions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.name").value("Search Page Spotlight"))
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.slotName").value("Search Page Spotlight"))
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.slotCode").value(SPOTLIGHT_SLOT_CODE))
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.description")
                        .value("Appear beside Tuition search results for high-visibility exposure."))
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.durationDays").value(30))
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.price").value(2490.00))
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.currentPrice").value(2490.00))
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.discounted").value(false));
    }

    @Test
    @Transactional
    void searchPageSpotlightIsRs990UnderTheLaunchCampaign() throws Exception {
        deactivateDefaultLaunchCampaign();
        long campaignId = promotionCampaignRepository.findByCode("EZCLASS_LAUNCH_990").orElseThrow().getId();
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/promotion-campaigns/" + campaignId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tuition/promotions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.price").value(2490.00))
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.currentPrice").value(990.00))
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.discounted").value(true));
    }

    @Test
    @Transactional
    void searchPageSpotlightIsRs1245UnderTheHalfPriceCampaign() throws Exception {
        deactivateDefaultLaunchCampaign();
        long campaignId = promotionCampaignRepository.findByCode("EZCLASS_HALF_PRICE").orElseThrow().getId();
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/promotion-campaigns/" + campaignId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tuition/promotions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.price").value(2490.00))
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.currentPrice").value(1245.00))
                .andExpect(jsonPath("$[?(@.plan.code == '" + SPOTLIGHT_PLAN_CODE + "')].plan.discounted").value(true));
    }

    @Test
    void otherSixBasePricesAreUnchangedByTheSpotlightRestoration() throws Exception {
        mockMvc.perform(get("/api/tuition/promotions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.plan.code == 'TUITION_SEARCH_TOP_30D')].plan.price").value(3490.00))
                .andExpect(jsonPath("$[?(@.plan.code == 'TUITION_SEARCH_BOOST_30D')].plan.price").value(2990.00))
                .andExpect(jsonPath("$[?(@.plan.code == 'TUITION_HOME_FEATURED_30D')].plan.price").value(2490.00))
                .andExpect(jsonPath("$[?(@.plan.code == 'TUITION_DETAIL_TOP_30D')].plan.price").value(1990.00))
                .andExpect(jsonPath("$[?(@.plan.code == 'TUITION_HOME_LATEST_RIGHT_30D')].plan.price").value(1490.00))
                .andExpect(jsonPath("$[?(@.plan.code == 'TUITION_DETAIL_RIGHT_30D')].plan.price").value(1490.00));
    }

    // --- purchase flow: buy Search Page Spotlight, verify it surfaces on the exact slot -----------

    @Test
    void promotingAClassOnSearchPageSpotlightSurfacesItOnlyOnThatExactSlot() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String title = "Search Spotlight Purchase " + UUID.randomUUID();

        long adId = createApprovedAd(kamalToken, title, "education-tuition");
        long planId = planIdByCode(kamalToken, SPOTLIGHT_PLAN_CODE);

        // The real EZCLASS_LAUNCH_FREE launch campaign (live by default since V27) makes this plan
        // free, so the purchase auto-activates immediately - no payment/admin-activation step, see
        // PromotionService#resolveCreationPlan.
        String createResponse = mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(createResponse);
        long promotionId = created.get("id").asLong();
        assertEquals(SPOTLIGHT_SLOT_CODE, created.get("slotCode").asText());
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(new BigDecimal(created.get("price").asText())));
        assertEquals("ACTIVE", created.get("status").asText());

        // Surfaces on the exact Search Page Spotlight slot...
        mockMvc.perform(get("/api/tuition/featured").param("slot", SPOTLIGHT_SLOT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")].title").value(title));

        // ...and nowhere else: never leaks into Search Top or Search Boost.
        mockMvc.perform(get("/api/tuition/featured").param("slot", "TUITION_SEARCH_TOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")]").doesNotExist());
        mockMvc.perform(get("/api/tuition/featured").param("slot", "TUITION_SEARCH_BOOST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")]").doesNotExist());

        // My promotions correctly reflects the plan/slot/channel actually purchased.
        mockMvc.perform(get("/api/tuition/promotions/my").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + promotionId + ")].promotionPlanCode").value(SPOTLIGHT_PLAN_CODE))
                .andExpect(jsonPath("$[?(@.id == " + promotionId + ")].slotCode").value(SPOTLIGHT_SLOT_CODE))
                .andExpect(jsonPath("$[?(@.id == " + promotionId + ")].status").value("ACTIVE"));
    }

    private long planIdByCode(String token, String code) throws Exception {
        String response = mockMvc.perform(get("/api/tuition/promotions/plans").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("plan").get("code").asText().equals(code)) {
                return node.get("plan").get("id").asLong();
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
