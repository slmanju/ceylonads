package com.slmanju.ceylonads.ad;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the phase-1 posting-model changes: ads have 0..N locations (category-dependent
 * cardinality) instead of exactly one, and generic MULTI_SELECT attributes accept multiple
 * deduplicated values while SINGLE_SELECT ones still reject more than one.
 *
 * Seeds explicitly via {@link LocalDataSeeder#run()}, same reasoning as AdQueryCountTests: this
 * suite doesn't rely on another test class happening to seed data first.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdLocationAndMultiSelectTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seed() throws Exception {
        seeder.run();
    }

    // --- Locations ------------------------------------------------------------------------

    @Test
    void servicesAdAcceptsZeroLocations() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        JsonNode ad = createAd(token, "services", List.of(),
                Map.of("serviceType", "Remote consulting"), status().isCreated());

        assertEquals(0, ad.get("locations").size());
    }

    @Test
    void onlineTuitionAcceptsZeroPhysicalLocations() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        JsonNode ad = createAd(token, "school-tuition", List.of(), tuitionAttrs("ONLINE"), status().isCreated());

        assertEquals(0, ad.get("locations").size());
    }

    @Test
    void onlineTuitionRejectsAPhysicalLocation() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        createAd(token, "school-tuition", List.of("colombo"), tuitionAttrs("ONLINE"), status().isBadRequest());
    }

    @Test
    void physicalTuitionRequiresAtLeastOneLocation() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        createAd(token, "school-tuition", List.of(), tuitionAttrs("PHYSICAL"), status().isBadRequest());
    }

    @Test
    void vehicleAdStillRequiresALocation() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        createAd(token, "cars", List.of(), carAttrs(), status().isBadRequest());
    }

    @Test
    void oneLocationPersistsCorrectly() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        JsonNode ad = createAd(token, "cars", List.of("colombo"), carAttrs(), status().isCreated());

        assertEquals(1, ad.get("locations").size());
        assertEquals("colombo", ad.get("locations").get(0).get("slug").asText());
    }

    @Test
    void multipleLocationsPersistCorrectly() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        JsonNode ad = createAd(token, "services", List.of("colombo", "kandy"),
                Map.of("serviceType", "Home tutoring"), status().isCreated());

        List<String> slugs = new java.util.ArrayList<>();
        ad.get("locations").forEach(l -> slugs.add(l.get("slug").asText()));
        assertEquals(2, slugs.size());
        assertTrue(slugs.contains("colombo"));
        assertTrue(slugs.contains("kandy"));
    }

    @Test
    void duplicateLocationSlugsAreDeduplicated() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        JsonNode ad = createAd(token, "services", List.of("colombo", "colombo", "COLOMBO"),
                Map.of("serviceType", "Home tutoring"), status().isCreated());

        assertEquals(1, ad.get("locations").size());
    }

    @Test
    void unknownLocationSlugIsRejected() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        createAd(token, "cars", List.of("not-a-real-location"), carAttrs(), status().isNotFound());
    }

    @Test
    void searchByLocationMatchesAnyOfAnAdsMultipleLocations() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "MultiLocMarker-" + UUID.randomUUID();

        Map<String, Object> body = new HashMap<>();
        body.put("title", marker + " service");
        body.put("description", "A description long enough for validation.");
        body.put("price", 1000);
        body.put("categorySlug", "services");
        body.put("locationSlugs", List.of("colombo", "kandy"));
        body.put("attributes", Map.of("serviceType", "Cleaning"));

        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long adId = objectMapper.readTree(response).get("id").asLong();

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + adId + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/ads").param("q", marker).param("location", "kandy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(adId));

        mockMvc.perform(get("/api/ads").param("q", marker).param("location", "colombo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(adId));
    }

    // --- MULTI_SELECT attributes ------------------------------------------------------------

    @Test
    void multiSelectAcceptsMultipleValues() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        JsonNode ad = createAd(token, "school-tuition", List.of("colombo"),
                tuitionAttrsWithMedium("ENGLISH,SINHALA"), status().isCreated());

        JsonNode medium = findAttribute(ad.get("attributes"), "medium");
        assertNotNull(medium);
        assertEquals("ENGLISH,SINHALA", medium.get("value").asText());
        assertEquals("English, Sinhala", medium.get("displayValue").asText());
    }

    @Test
    void multiSelectDeduplicatesRepeatedValues() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        JsonNode ad = createAd(token, "school-tuition", List.of("colombo"),
                tuitionAttrsWithMedium("ENGLISH,english,ENGLISH"), status().isCreated());

        JsonNode medium = findAttribute(ad.get("attributes"), "medium");
        assertEquals("ENGLISH", medium.get("value").asText());
    }

    // --- Optional price / description -------------------------------------------------------

    @Test
    void blankDescriptionIsAcceptedAsAnEmptyListing() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Blank Description Test " + UUID.randomUUID());
        body.put("description", "");
        body.put("price", 0);
        body.put("categorySlug", "services");
        body.put("locationSlugs", List.of());
        body.put("attributes", Map.of("serviceType", "Cleaning"));

        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertEquals("", objectMapper.readTree(response).get("description").asText());
    }

    @Test
    void singleSelectStillRejectsMultipleValues() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        Map<String, String> attrs = carAttrs();
        attrs.put("fuelType", "PETROL,HYBRID");

        createAd(token, "cars", List.of("colombo"), attrs, status().isBadRequest());
    }

    // --- helpers ----------------------------------------------------------------------------

    private Map<String, String> tuitionAttrs(String classMode) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("subject", "Mathematics");
        attrs.put("curriculum", "LOCAL");
        attrs.put("medium", "ENGLISH");
        attrs.put("classMode", classMode);
        return attrs;
    }

    private Map<String, String> tuitionAttrsWithMedium(String medium) {
        Map<String, String> attrs = tuitionAttrs("PHYSICAL");
        attrs.put("medium", medium);
        return attrs;
    }

    private Map<String, String> carAttrs() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("make", "Toyota");
        attrs.put("model", "Aqua");
        attrs.put("year", "2019");
        attrs.put("mileage", "72000");
        attrs.put("fuelType", "HYBRID");
        attrs.put("transmission", "AUTOMATIC");
        return attrs;
    }

    private JsonNode createAd(
            String token, String categorySlug, List<String> locationSlugs,
            Map<String, String> attributes, org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Location Test Ad " + UUID.randomUUID());
        body.put("description", "A description long enough for validation.");
        body.put("price", 1000);
        body.put("categorySlug", categorySlug);
        body.put("locationSlugs", locationSlugs);
        body.put("attributes", attributes);

        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(expectedStatus)
                .andReturn().getResponse().getContentAsString();
        return response.isBlank() ? null : objectMapper.readTree(response);
    }

    private JsonNode findAttribute(JsonNode attributes, String key) {
        for (JsonNode attribute : attributes) {
            if (attribute.get("key").asText().equals(key)) {
                return attribute;
            }
        }
        return null;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
