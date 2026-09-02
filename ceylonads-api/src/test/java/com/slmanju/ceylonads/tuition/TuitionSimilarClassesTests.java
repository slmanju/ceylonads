package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises GET /api/tuition/classes/{slug}/similar. Creates fresh TUITION-channel classes through
 * the real Tuition create API (rather than depending on a seeded fixture) so the candidate pool is
 * guaranteed to actually be source_channel = TUITION, matching what the similar-classes query now
 * requires.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionSimilarClassesTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seed() throws Exception {
        seeder.run();
    }

    @Test
    void similarClassesExcludesTheCurrentAdAndDefaultsToThree() throws Exception {
        List<Long> ids = createTuitionClassesInSameCategory(4);
        long adId = ids.get(0);

        String response = mockMvc.perform(get("/api/tuition/classes/" + adId + "/similar"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode cards = objectMapper.readTree(response);

        assertTrue(cards.size() <= 3, "default size should be capped at 3, got " + cards.size());
        for (JsonNode card : cards) {
            assertFalse(card.get("id").asLong() == adId, "similar classes must not include the class itself");
        }
    }

    @Test
    void similarClassesRespectsRequestedSize() throws Exception {
        List<Long> ids = createTuitionClassesInSameCategory(4);
        long adId = ids.get(0);

        String response = mockMvc.perform(get("/api/tuition/classes/" + adId + "/similar").param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode cards = objectMapper.readTree(response);

        assertEquals(1, cards.size());
    }

    @Test
    void similarClassesCardShapeOmitsHeavyFields() throws Exception {
        List<Long> ids = createTuitionClassesInSameCategory(2);
        long adId = ids.get(0);

        String response = mockMvc.perform(get("/api/tuition/classes/" + adId + "/similar").param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode card = objectMapper.readTree(response).get(0);

        assertTrue(card.has("id"));
        assertTrue(card.has("slug"));
        assertTrue(card.has("title"));
        assertTrue(card.has("price"));
        // Card DTO deliberately has no description/media-list/seller/full-attribute fields.
        assertFalse(card.has("description"));
        assertFalse(card.has("media"));
        assertFalse(card.has("seller"));
    }

    // Creates `count` approved TUITION classes in the same leaf category (education-tuition, which
    // has no required attributes, so no attribute payload is needed) via the real Tuition create
    // API - the candidate pool for GET .../similar now requires source_channel = TUITION, so a
    // seeded MAIN_SITE fixture ad can no longer stand in for it.
    private List<Long> createTuitionClassesInSameCategory(int count) throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "SimilarClassesMarker-" + UUID.randomUUID();
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
