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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers GET /api/tuition/classes/search's Search Boost (TUITION_SEARCH_BOOST) ranking: a matching
// ad with a currently active Search Boost promotion is ranked first among the SAME organic results
// (never a separate section/endpoint, never additive to size, never bypassing active filters) and
// comes back with promoted=true - see AdSearchService's slot-code overload and
// TuitionClassService.search. Uses a unique `q` marker per test (like TuitionSearchPaginationTests)
// to scope "matching" without needing real per-category SELECT attribute fixtures.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionSearchBoostRankingTests {

    private static final String CATEGORY_SLUG = "school-tuition";
    private static final String BOOST_PLAN_CODE = "TUITION_SEARCH_BOOST_30D";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

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
    void aSingleMatchingBoostedClassIsTheOnlyAndFirstResult() throws Exception {
        String marker = "BoostSingle-" + UUID.randomUUID();
        long adId = persistAd(marker + " O/L Science");
        activateSearchBoost(adId);

        mockMvc.perform(get("/api/tuition/classes/search").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(adId))
                .andExpect(jsonPath("$.content[0].promoted").value(true));
    }

    @Test
    void boostedMatchRanksBeforeNonBoostedMatch() throws Exception {
        String marker = "BoostOrder-" + UUID.randomUUID();
        long boostedId = persistAd(marker + " Spoken English Classes");
        long normalId = persistAd(marker + " English for Everyone");
        activateSearchBoost(boostedId);

        mockMvc.perform(get("/api/tuition/classes/search").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(boostedId))
                .andExpect(jsonPath("$.content[0].promoted").value(true))
                .andExpect(jsonPath("$.content[1].id").value(normalId))
                .andExpect(jsonPath("$.content[1].promoted").value(false));
    }

    @Test
    void aBoostedClassThatDoesNotMatchTheFiltersIsNotReturned() throws Exception {
        String matchingMarker = "BoostFilterMatch-" + UUID.randomUUID();
        String otherMarker = "BoostFilterOther-" + UUID.randomUUID();
        long matchingId = persistAd(matchingMarker + " Spoken English Classes");
        long unrelatedBoostedId = persistAd(otherMarker + " A/L Physics");
        activateSearchBoost(unrelatedBoostedId);

        mockMvc.perform(get("/api/tuition/classes/search").param("q", matchingMarker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matchingId))
                .andExpect(jsonPath("$.content[?(@.id == " + unrelatedBoostedId + ")]").doesNotExist());
    }

    @Test
    void paginationRanksBoostedFirstAcrossPagesWithoutDuplicatesOrGaps() throws Exception {
        String marker = "BoostPagination-" + UUID.randomUUID();
        Set<Long> boostedIds = new HashSet<>();
        Set<Long> allIds = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            long id = persistAd(marker + " class " + i);
            allIds.add(id);
            if (i < 3) {
                activateSearchBoost(id);
                boostedIds.add(id);
            }
        }

        String page0Response = mockMvc.perform(get("/api/tuition/classes/search").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(9))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andReturn().getResponse().getContentAsString();

        JsonNode page0Content = objectMapper.readTree(page0Response).get("content");
        Set<Long> page0Ids = new HashSet<>();
        for (int i = 0; i < page0Content.size(); i++) {
            JsonNode item = page0Content.get(i);
            long id = item.get("id").asLong();
            page0Ids.add(id);
            boolean shouldBeBoosted = i < 3;
            assertEquals(shouldBeBoosted, item.get("promoted").asBoolean(),
                    "position " + i + " (id " + id + ") promoted flag");
            if (shouldBeBoosted) {
                assertTrue(boostedIds.contains(id), "the first 3 page-1 results must be the 3 boosted ads");
            }
        }

        String page1Response = mockMvc.perform(get("/api/tuition/classes/search").param("q", marker).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andReturn().getResponse().getContentAsString();

        JsonNode page1Content = objectMapper.readTree(page1Response).get("content");
        Set<Long> page1Ids = new HashSet<>();
        for (JsonNode item : page1Content) {
            assertFalse(item.get("promoted").asBoolean(), "page 2 must contain only normal (non-boosted) results");
            page1Ids.add(item.get("id").asLong());
        }

        assertTrue(java.util.Collections.disjoint(page0Ids, page1Ids), "no ad should appear on both pages");
        Set<Long> combined = new HashSet<>(page0Ids);
        combined.addAll(page1Ids);
        assertEquals(allIds, combined, "every matching ad must appear exactly once across both pages");
    }

    @Test
    @Transactional
    void anExpiredSearchBoostNoLongerRanksOrBadgesButTheClassStillAppears() throws Exception {
        String marker = "BoostExpired-" + UUID.randomUUID();
        long expiredBoostId = persistAd(marker + " Expired Boost Class");
        long plainId = persistAd(marker + " Plain Class");
        long promotionId = activateSearchBoost(expiredBoostId);

        entityManager.createQuery("update Promotion p set p.endsAt = :past where p.id = :id")
                .setParameter("past", Instant.now().minusSeconds(60))
                .setParameter("id", promotionId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/tuition/classes/search").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.id == " + expiredBoostId + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + expiredBoostId + ")].promoted").value(false))
                .andExpect(jsonPath("$.content[?(@.id == " + plainId + ")].promoted").value(false));
    }

    // Buys and activates a TUITION_SEARCH_BOOST_30D promotion on the given ad (owned by "kamal" -
    // see persistAd), returning the created promotion's id. A customer-initiated request always
    // requires admin moderation (see PromotionService#resolveCreationPlan) even though the real
    // EZCLASS_LAUNCH_FREE launch campaign (live by default since V27) makes this plan free - FREE
    // only zeroes the charged price, it never bypasses approval, so this always goes through the
    // Tuition-scoped approve endpoint rather than assuming creation already activated it.
    private long activateSearchBoost(long adId) throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long planId = planIdByCode(kamalToken, BOOST_PLAN_CODE);

        String createResponse = mockMvc.perform(post("/api/tuition/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", adId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(createResponse);
        long promotionId = created.get("id").asLong();

        if (!"ACTIVE".equals(created.get("status").asText())) {
            mockMvc.perform(patch("/api/admin/tuition/promotions/" + promotionId + "/approve")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
        return promotionId;
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

    // Direct repository persistence (like TuitionSearchPaginationTests) rather than the create-ad
    // API - faster for building many fixtures, and Search Boost purchase only needs a real,
    // ACTIVE, TUITION-channel ad owned by "kamal", not the full create/approve flow.
    private long persistAd(String title) {
        Category category = categories.findBySlug(CATEGORY_SLUG).orElseThrow();
        Customer seller = customers.findByAccountUsernameIgnoreCase("kamal").orElseThrow();
        Ad ad = new Ad(title, "A description long enough for validation.", new BigDecimal("1000"), category, seller);
        ad.assignSourceChannel(SourceChannel.TUITION);
        ad.approve(null);
        return ads.save(ad).getId();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
