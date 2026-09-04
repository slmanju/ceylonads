package com.slmanju.ceylonads.promotion;

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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * §28 20-21: the server, never the client, resolves the charged price at purchase time, and that
 * charged price is snapshotted onto the Promotion row so a later campaign change never rewrites
 * an already-sold promotion's price. Exercises AdminPromotionCampaignController end-to-end against
 * a real Tuition promotion plan.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class PromotionCampaignPurchaseSnapshotTests {

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

    @Test
    @Transactional
    void purchasePriceIsServerResolvedAndSurvivesLaterCampaignChanges() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");

        // The real EZCLASS_LAUNCH_FREE launch campaign (live by default since V27) already covers
        // this plan/channel/window, so it must step aside for this test's own throwaway fixed-price
        // campaign - rolled back automatically at the end since this test is @Transactional.
        long launchId = promotionCampaignRepository.findByCode("EZCLASS_LAUNCH_FREE").orElseThrow().getId();
        mockMvc.perform(patch("/api/admin/promotion-campaigns/" + launchId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        long adId = createApprovedTuitionClass(kamalToken, "Snapshot Class " + UUID.randomUUID());
        long planId = planIdByCode(kamalToken, adId, "TUITION_HOME_FEATURED_30D");

        long campaignId = createCampaign(adminToken, planId, "PURCHASE_SNAPSHOT_TEST", "990.00");

        // The client only ever supplies a promotionPlanId - never a price - and the amount charged
        // must reflect the active campaign's fixed price, not the plan's base price.
        String createResponse = mockMvc.perform(post("/api/tuition/my-classes/" + adId + "/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(createResponse);
        long promotionId = created.get("id").asLong();
        assertEquals(0, new BigDecimal("990.00").compareTo(new BigDecimal(created.get("price").asText())));

        // Deactivating the campaign after the fact must not rewrite the already-sold promotion's
        // charged price, even though a fresh catalog lookup would now show the base price again.
        mockMvc.perform(patch("/api/admin/promotion-campaigns/" + campaignId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String reFetched = mockMvc.perform(get("/api/promotions/" + promotionId)
                        .header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        BigDecimal snapshotPrice = new BigDecimal(objectMapper.readTree(reFetched).get("price").asText());
        assertEquals(0, new BigDecimal("990.00").compareTo(snapshotPrice), "charged price must remain the snapshot, not revert to base price");
    }

    private long createCampaign(String adminToken, long planId, String code, String fixedPrice) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", code);
        request.put("name", "Purchase Snapshot Test Campaign");
        request.put("description", "Test-only campaign.");
        request.put("sourceChannel", "TUITION");
        request.put("pricingType", "FIXED_PRICE");
        request.put("fixedPrice", fixedPrice);
        request.put("startsAt", Instant.now().minus(1, ChronoUnit.HOURS).toString());
        request.put("endsAt", Instant.now().plus(1, ChronoUnit.HOURS).toString());
        request.put("planIds", List.of(planId));
        String response = mockMvc.perform(post("/api/admin/promotion-campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createApprovedTuitionClass(String token, String title) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "A description long enough for validation purposes.");
        body.put("price", 3000);
        body.put("categorySlug", "school-tuition");
        body.put("locationSlugs", List.of("colombo"));
        body.put("subject", "Combined Mathematics");
        body.put("level", "A/L");
        body.put("curriculum", "LOCAL");
        body.put("medium", List.of("ENGLISH"));
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

    private long planIdByCode(String token, long adId, String code) throws Exception {
        String response = mockMvc.perform(get("/api/tuition/my-classes/" + adId + "/promotion-plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("plan").get("code").asText().equals(code)) {
                return node.get("plan").get("id").asLong();
            }
        }
        throw new IllegalStateException("Compatible plan not found: " + code);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
