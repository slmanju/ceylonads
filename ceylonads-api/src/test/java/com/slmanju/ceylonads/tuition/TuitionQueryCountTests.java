package com.slmanju.ceylonads.tuition;

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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression guard for the tuition read paths' query-count budget, mirrors
 * {@code com.slmanju.ceylonads.ad.AdQueryCountTests}: asserts an upper bound on SQL statements
 * Hibernate actually issues rather than exact SQL text.
 *
 * Target detail shape: ad+category+parent+seller (1), media (1), attribute values+definitions (1),
 * attribute options for select-type values only (0-1), locations (1) - no generic search, category
 * hierarchy walk, promotion lookup, or count query.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionQueryCountTests {

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
    void tuitionClassDetailUsesABoundedQueryCount() throws Exception {
        long adId = createTuitionClassesInSameCategory(1).get(0);

        statistics.clear();
        mockMvc.perform(get("/api/tuition/classes/" + adId)).andExpect(status().isOk());

        // A handful of slack is allowed for the transaction/connection-handling statements Spring
        // and Hibernate issue around the actual query work.
        assertBoundedQueryCount(6, "GET /api/tuition/classes/{id}");
    }

    @Test
    void tuitionSimilarClassesDoesNotGrowQueryCountWithRequestedSize() throws Exception {
        long adId = createTuitionClassesInSameCategory(3).get(0);

        statistics.clear();
        mockMvc.perform(get("/api/tuition/classes/" + adId + "/similar").param("size", "1"))
                .andExpect(status().isOk());
        long queriesForSizeOne = statistics.getPrepareStatementCount();

        statistics.clear();
        mockMvc.perform(get("/api/tuition/classes/" + adId + "/similar").param("size", "10"))
                .andExpect(status().isOk());
        long queriesForSizeTen = statistics.getPrepareStatementCount();

        // The candidate pool is fetched once (capped at 20) regardless of the requested page size;
        // ranking/truncation happens in memory, so query count should stay flat.
        assertTrue(queriesForSizeTen - queriesForSizeOne <= 1,
                "expected similar-classes query count to stay flat across sizes, size=1 -> " + queriesForSizeOne
                        + ", size=10 -> " + queriesForSizeTen);
        assertBoundedQueryCount(8, "GET /api/tuition/classes/{id}/similar");
    }

    private void assertBoundedQueryCount(long maxQueries, String label) {
        long actual = statistics.getPrepareStatementCount();
        assertTrue(actual <= maxQueries, label + " issued " + actual + " SQL statements, expected <= " + maxQueries);
    }

    // Creates `count` approved TUITION classes via the real create API - the query-count budget
    // this suite guards now runs against genuine source_channel = TUITION rows, not a seeded
    // MAIN_SITE fixture ad, since the tuition read paths require TUITION at the query level.
    private List<Long> createTuitionClassesInSameCategory(int count) throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "QueryCountMarker-" + UUID.randomUUID();
        List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            String body = objectMapper.writeValueAsString(Map.of(
                    "title", marker + " class " + i,
                    "description", "A description long enough for validation.",
                    "price", new BigDecimal("1000"),
                    "categorySlug", "education-tuition",
                    "locationSlugs", List.of("colombo")));
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
            ids.add(id);
        }
        return ids;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
