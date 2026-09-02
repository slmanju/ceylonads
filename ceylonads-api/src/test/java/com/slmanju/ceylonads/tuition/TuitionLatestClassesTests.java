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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises GET /api/tuition/classes - the isolated, tuition-only "Latest Classes" listing (see
 * TuitionClassService.getLatest). Unlike GET /api/tuition/featured, this is genuine page-by-page
 * browsing, so it legitimately needs and returns a real totalPages/totalElements.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionLatestClassesTests {

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
    void defaultsToPageZeroSizeSix() throws Exception {
        String response = mockMvc.perform(get("/api/tuition/classes"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);

        assertEquals(0, body.get("page").asInt());
        assertEquals(6, body.get("size").asInt());
        assertTrue(body.get("content").size() <= 6, "expected at most 6 cards, got " + body.get("content").size());
    }

    @Test
    void respectsRequestedSize() throws Exception {
        String response = mockMvc.perform(get("/api/tuition/classes").param("size", "2"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);

        assertEquals(2, body.get("content").size());
    }

    @Test
    void onlyReturnsAdsFromTheTuitionCategoryTree() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String marker = "LatestClassesMarker-" + UUID.randomUUID();

        long tuitionAdId = createApprovedTuitionClass(kamalToken, marker + " tuition");
        long vehicleAdId = createApprovedAd(kamalToken, marker + " vehicle", "vehicles");

        String response = mockMvc.perform(get("/api/tuition/classes").param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(response).get("content");

        boolean sawTuitionAd = false;
        for (JsonNode card : content) {
            long id = card.get("id").asLong();
            assertFalse(id == vehicleAdId, "a non-tuition ad must never appear in the tuition latest-classes feed");
            if (id == tuitionAdId) {
                sawTuitionAd = true;
            }
        }
        assertTrue(sawTuitionAd, "the newly created tuition ad should appear on the first (newest) page");
    }

    @Test
    void ordersNewestFirst() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String marker = "LatestClassesOrderMarker-" + UUID.randomUUID();

        long olderAdId = createApprovedTuitionClass(kamalToken, marker + " older");
        Thread.sleep(5);
        long newerAdId = createApprovedTuitionClass(kamalToken, marker + " newer");

        String response = mockMvc.perform(get("/api/tuition/classes").param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(response).get("content");

        int newerIndex = -1;
        int olderIndex = -1;
        for (int i = 0; i < content.size(); i++) {
            long id = content.get(i).get("id").asLong();
            if (id == newerAdId) newerIndex = i;
            if (id == olderAdId) olderIndex = i;
        }
        assertTrue(newerIndex >= 0 && olderIndex >= 0, "both ads should be present on the first page");
        assertTrue(newerIndex < olderIndex, "the newer ad should be ranked before the older one");
    }

    @Test
    void cardShapeOmitsHeavyFields() throws Exception {
        String response = mockMvc.perform(get("/api/tuition/classes").param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(response).get("content");
        assumeAtLeastOneCard(content);
        JsonNode card = content.get(0);

        assertTrue(card.has("id"));
        assertTrue(card.has("slug"));
        assertTrue(card.has("title"));
        assertTrue(card.has("price"));
        assertFalse(card.has("description"));
        assertFalse(card.has("media"));
        assertFalse(card.has("seller"));
    }

    private void assumeAtLeastOneCard(JsonNode content) {
        assertTrue(content.size() > 0, "expected at least one seeded tuition class to assert the card shape against");
    }

    // Uses the dedicated Tuition create API (POST /api/tuition/classes) rather than the generic
    // /api/ads, so the resulting ad is actually source_channel = TUITION - the same isolation
    // GET /api/tuition/classes itself now enforces. "education-tuition" (the tree root) has no
    // required attribute_definitions, so an empty attribute set is valid here.
    private long createApprovedTuitionClass(String token, String title) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1000"),
                "categorySlug", "education-tuition",
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
