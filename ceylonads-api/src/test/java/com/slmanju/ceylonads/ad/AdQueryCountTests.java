package com.slmanju.ceylonads.ad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression guard for the N+1 fixes in the ad read paths: asserts an upper bound on the number
 * of SQL statements Hibernate actually issues, rather than exact SQL text, so these keep passing
 * across incidental query rewrites as long as the query count stays flat.
 *
 * Seeds explicitly via {@link LocalDataSeeder#run()} (the same call {@code SeederController}
 * exposes) instead of relying on data being present already, since nothing in this test module
 * seeds automatically on startup.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdQueryCountTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Statistics statistics;

    @BeforeEach
    void seedAndResetStatistics() throws Exception {
        seeder.run();
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @Test
    void singleAdDetailUsesABoundedQueryCount() throws Exception {
        long adId = firstActiveAdId();

        statistics.clear();
        mockMvc.perform(get("/api/ads/" + adId)).andExpect(status().isOk());

        // Target shape: ad+category+location+seller, media, attributes+definitions+options.
        // A handful of slack is allowed for the transaction/connection-handling statements Spring
        // and Hibernate issue around the actual query work.
        assertBoundedQueryCount(5, "GET /api/ads/{id}");
    }

    @Test
    void searchResultsPageDoesNotGrowQueryCountWithPageSize() throws Exception {
        statistics.clear();
        mockMvc.perform(get("/api/ads").param("size", "2")).andExpect(status().isOk());
        long queriesForSmallPage = statistics.getPrepareStatementCount();

        statistics.clear();
        mockMvc.perform(get("/api/ads").param("size", "20")).andExpect(status().isOk());
        long queriesForLargePage = statistics.getPrepareStatementCount();

        // Under the old per-ad mapper (media + attribute-value + attribute-option queries per
        // ad), a 10x larger page would issue roughly 10x more statements. With batching, both
        // page sizes should cost about the same fixed number of round trips.
        long delta = queriesForLargePage - queriesForSmallPage;
        assertTrue(delta <= 3,
                "expected search result count to stay flat across page sizes, small page = "
                        + queriesForSmallPage + ", large page = " + queriesForLargePage);
    }

    @Test
    void myAdsListUsesABoundedQueryCountRegardlessOfAdCount() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        statistics.clear();
        mockMvc.perform(get("/api/ads/mine").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // kamal owns several seeded ads across multiple categories/attribute sets; under the old
        // per-ad mapper this would scale with ad count instead of staying flat. One more than
        // before locations became 0..N: locations are now a fourth batched query alongside
        // media/attributes, same reasoning as those two.
        assertBoundedQueryCount(7, "GET /api/ads/mine");
    }

    @Test
    void categoryFeaturedCarouselUsesABoundedQueryCount() throws Exception {
        statistics.clear();
        mockMvc.perform(get("/api/ads/category-featured").param("categorySlug", "cars"))
                .andExpect(status().isOk());

        // A couple of extra statements versus the other endpoints here are the (ad-count
        // independent) category/slot resolution walk that precedes the promoted-ads query itself.
        assertBoundedQueryCount(8, "GET /api/ads/category-featured");
    }

    private void assertBoundedQueryCount(long maxQueries, String label) {
        long actual = statistics.getPrepareStatementCount();
        assertTrue(actual <= maxQueries, label + " issued " + actual + " SQL statements, expected <= " + maxQueries);
    }

    private long firstActiveAdId() throws Exception {
        String response = mockMvc.perform(get("/api/ads").param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(response).get("content");
        return content.get(0).get("id").asLong();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("username", username, "password", password));
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
