package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers My Classes' "Promote" action (GET/POST /api/tuition/my-classes/{adId}/promotion-plans,
// .../promotions): reuses the shared promotion tables/service, adding only a source_channel=TUITION
// ownership check (PromotionService#compatiblePlansForTuitionAd/#createForTuitionAd) so a tutor can
// never promote a MAIN_SITE/BOARDING ad through this path, even by id.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionMyClassPromotionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private AdRepository ads;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private CustomerRepository customers;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void ownerSeesCompatiblePlansForActiveTuitionClass() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Compatible Plans Class " + UUID.randomUUID()));
        approveAsAdmin(id);

        mockMvc.perform(get("/api/tuition/my-classes/" + id + "/promotion-plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.plan.code == 'TUITION_HOME_FEATURED_30D')]").exists());
    }

    // Catalog cleanup regression coverage (§28 22-24 of the promotion catalog/pricing refactor):
    // only the six final Tuition products are exposed, no generic/obsolete plan leaks in, and the
    // response carries the new pricing fields.
    @Test
    void catalogExposesOnlyTheSixTuitionProductsWithPricingFields() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Catalog Cleanup Class " + UUID.randomUUID()));
        approveAsAdmin(id);

        String response = mockMvc.perform(get("/api/tuition/my-classes/" + id + "/promotion-plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        java.util.Set<String> expectedCodes = java.util.Set.of(
                "TUITION_SEARCH_TOP_30D", "TUITION_SEARCH_BOOST_30D", "TUITION_HOME_FEATURED_30D",
                "TUITION_DETAIL_TOP_30D", "TUITION_HOME_LATEST_RIGHT_30D", "TUITION_DETAIL_RIGHT_30D");
        java.util.Set<String> retiredOrGenericCodes = java.util.Set.of(
                "HOME_FEATURED_7D", "HOME_FEATURED_30D", "TOP_SEARCH_7D", "DETAIL_SIDEBAR_FEATURED",
                "TUITION_FEATURED_7D", "TUITION_DETAIL_TOP_CAROUSEL_7D",
                "TUITION_SEARCH_SIDEBAR_TOP_7D", "TUITION_SEARCH_SIDEBAR_MIDDLE_7D", "TUITION_SEARCH_SIDEBAR_BOTTOM_7D");

        for (JsonNode node : objectMapper.readTree(response)) {
            JsonNode plan = node.get("plan");
            String code = plan.get("code").asText();
            assertFalse(retiredOrGenericCodes.contains(code), "retired/generic plan leaked into catalog: " + code);
            assertTrue(expectedCodes.contains(code), "unexpected plan in Tuition catalog: " + code);
            assertTrue(plan.has("currentPrice"));
            assertTrue(plan.has("discounted"));
        }
    }

    @Test
    void ownerCanPromoteActiveTuitionClass() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Promote Active Class " + UUID.randomUUID()));
        approveAsAdmin(id);
        long planId = planIdByCode(token, id, "TUITION_HOME_FEATURED_30D");

        // The real EZCLASS_LAUNCH_FREE launch campaign (live by default since V27) makes this plan
        // free, but FREE only zeroes the price - a customer-initiated request still requires admin
        // approval (see PromotionService#resolveCreationPlan), so this endpoint's own job (creating
        // the request) is proven by PENDING_APPROVAL, not an immediate ACTIVE.
        mockMvc.perform(post("/api/tuition/my-classes/" + id + "/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adId").value(id))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    void pendingReviewClassCannotBePromoted() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Pending Class " + UUID.randomUUID()));
        // Not approved - still PENDING_REVIEW. Read the plan through the generic endpoint since the
        // scoped one also requires ownership+channel, both of which are satisfied here.
        long planId = genericPlanIdByCode(token, "TUITION_HOME_FEATURED_30D");

        mockMvc.perform(post("/api/tuition/my-classes/" + id + "/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", planId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivatedClassCannotBePromoted() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Deactivated Class " + UUID.randomUUID()));
        approveAsAdmin(id);
        long planId = planIdByCode(token, id, "TUITION_HOME_FEATURED_30D");

        mockMvc.perform(delete("/api/tuition/classes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/tuition/my-classes/" + id + "/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", planId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherTutorsClassCannotBePromoted() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(kamalToken, tuitionBody("Not Yours Promote Class " + UUID.randomUUID()));
        approveAsAdmin(id);

        String nimalToken = loginAndGetToken("nimal", "customer123");
        long planId = genericPlanIdByCode(nimalToken, "TUITION_HOME_FEATURED_30D");

        mockMvc.perform(post("/api/tuition/my-classes/" + id + "/promotions")
                        .header("Authorization", "Bearer " + nimalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", planId))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tuition/my-classes/" + id + "/promotion-plans")
                        .header("Authorization", "Bearer " + nimalToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void tuitionEndpointRejectsMainSiteAdEvenWhenOwnedAndActive() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long mainSiteId = createApprovedMainSiteAd(token, "Main Site For Tuition Promote " + UUID.randomUUID());
        long planId = genericPlanIdByCode(token, "VEHICLES_FEATURED_7D");

        mockMvc.perform(get("/api/tuition/my-classes/" + mainSiteId + "/promotion-plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/tuition/my-classes/" + mainSiteId + "/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", planId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void tuitionEndpointRejectsBoardingAdEvenWhenOwnedAndActive() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long boardingId = persistActiveChannelAd(SourceChannel.BOARDING, "vehicles", "Boarding For Tuition Promote " + UUID.randomUUID());
        long planId = genericPlanIdByCode(token, "VEHICLES_FEATURED_7D");

        mockMvc.perform(post("/api/tuition/my-classes/" + boardingId + "/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("promotionPlanId", planId))))
                .andExpect(status().isNotFound());
    }

    // --- helpers --------------------------------------------------------------------------------

    private Map<String, Object> tuitionBody(String title) {
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

    // Direct repository insert (already-ACTIVE), mirroring TuitionClassLifecycleTests#persistChannelAd
    // - there is still no client-facing way to create a BOARDING ad.
    private long persistActiveChannelAd(SourceChannel channel, String categorySlug, String title) {
        Category category = categories.findBySlug(categorySlug).orElseThrow();
        Customer seller = customers.findByAccountUsernameIgnoreCase("kamal").orElseThrow();
        Ad ad = new Ad(title, "A description long enough for validation.", new BigDecimal("1000"), category, seller);
        ad.assignSourceChannel(channel);
        ad.approve(null);
        return ads.save(ad).getId();
    }

    private void approveAsAdmin(long id) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
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
