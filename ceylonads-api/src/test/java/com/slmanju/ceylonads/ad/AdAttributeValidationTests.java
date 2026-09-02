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
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdAttributeValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Doesn't rely on another test class in the shared context having seeded first - this suite
    // must be self-sufficient regardless of JUnit/Gradle test-class discovery order.
    @BeforeEach
    void seed() throws Exception {
        seeder.run();
    }

    private Map<String, String> validCarAttributes() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("make", "Toyota");
        attrs.put("model", "Aqua");
        attrs.put("year", "2019");
        attrs.put("mileage", "72000");
        attrs.put("fuelType", "HYBRID");
        attrs.put("transmission", "AUTOMATIC");
        return attrs;
    }

    @Test
    void requiredAttributeMissingIsRejected() throws Exception {
        Map<String, String> attrs = validCarAttributes();
        attrs.remove("make");
        String token = loginAndGetToken("kamal", "customer123");

        mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(adBody("cars", attrs)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAttributeKeyIsRejected() throws Exception {
        Map<String, String> attrs = validCarAttributes();
        attrs.put("notARealAttribute", "value");
        String token = loginAndGetToken("kamal", "customer123");

        mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(adBody("cars", attrs)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongDataTypeIsRejected() throws Exception {
        Map<String, String> attrs = validCarAttributes();
        attrs.put("year", "not-a-number");
        String token = loginAndGetToken("kamal", "customer123");

        mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(adBody("cars", attrs)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSelectOptionIsRejected() throws Exception {
        Map<String, String> attrs = validCarAttributes();
        attrs.put("fuelType", "NUCLEAR");
        String token = loginAndGetToken("kamal", "customer123");

        mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(adBody("cars", attrs)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validVehicleAttributesArePersistedAndReturnedInAdResponse() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token, "cars", validCarAttributes());

        JsonNode attributes = attributesOfMine(token, adId);
        org.junit.jupiter.api.Assertions.assertEquals("Toyota", attributeValue(attributes, "make"));
        org.junit.jupiter.api.Assertions.assertEquals("2019", attributeValue(attributes, "year"));
        org.junit.jupiter.api.Assertions.assertEquals("Hybrid", attributeDisplayValue(attributes, "fuelType"));
    }

    @Test
    void validPropertyAttributesArePersisted() throws Exception {
        Map<String, String> attrs = Map.of(
                "propertyType", "HOUSE", "bedrooms", "4", "bathrooms", "3",
                "landSize", "10.5", "floorArea", "2400");
        String token = loginAndGetToken("nimal", "customer123");
        long adId = createAd(token, "houses", attrs);

        JsonNode attributes = attributesOfMine(token, adId);
        org.junit.jupiter.api.Assertions.assertEquals("House", attributeDisplayValue(attributes, "propertyType"));
        org.junit.jupiter.api.Assertions.assertEquals("4", attributeValue(attributes, "bedrooms"));
        org.junit.jupiter.api.Assertions.assertEquals("perches", findAttribute(attributes, "landSize").get("unit").asText());
    }

    @Test
    void validTuitionAttributesArePersisted() throws Exception {
        Map<String, String> attrs = Map.of(
                "subject", "Mathematics", "curriculum", "LOCAL", "medium", "ENGLISH", "classMode", "BOTH");
        String token = loginAndGetToken("nimal", "customer123");
        long adId = createAd(token, "school-tuition", attrs);

        JsonNode attributes = attributesOfMine(token, adId);
        org.junit.jupiter.api.Assertions.assertEquals("Mathematics", attributeValue(attributes, "subject"));
        org.junit.jupiter.api.Assertions.assertEquals("English", attributeDisplayValue(attributes, "medium"));
    }

    @Test
    void editingAnAdUpdatesItsAttributeValues() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token, "cars", validCarAttributes());

        Map<String, String> updated = validCarAttributes();
        updated.put("model", "Prius");
        updated.put("year", "2021");
        mockMvc.perform(put("/api/ads/" + adId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(adBody("cars", updated)))
                .andExpect(status().isOk());

        JsonNode attributes = attributesOfMine(token, adId);
        org.junit.jupiter.api.Assertions.assertEquals("Prius", attributeValue(attributes, "model"));
        org.junit.jupiter.api.Assertions.assertEquals("2021", attributeValue(attributes, "year"));
    }

    @Test
    void changingCategoryReplacesIncompatibleAttributeValues() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token, "cars", validCarAttributes());

        Map<String, String> houseAttrs = Map.of("propertyType", "APARTMENT", "bedrooms", "2", "bathrooms", "1");
        mockMvc.perform(put("/api/ads/" + adId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(adBody("houses", houseAttrs)))
                .andExpect(status().isOk());

        JsonNode attributes = attributesOfMine(token, adId);
        org.junit.jupiter.api.Assertions.assertNull(findAttribute(attributes, "make"));
        org.junit.jupiter.api.Assertions.assertEquals("APARTMENT", attributeValue(attributes, "propertyType"));
    }

    private long createAd(String token, String categorySlug, Map<String, String> attributes) throws Exception {
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(adBody(categorySlug, attributes)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    // New ads sit in PENDING_REVIEW, which the public GET /api/ads/{id} hides - /mine is the
    // owner-visible listing that includes non-active ads, same as AdSecurityTests uses.
    private JsonNode attributesOfMine(String token, long adId) throws Exception {
        String response = mockMvc.perform(get("/api/ads/mine").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode ad : objectMapper.readTree(response)) {
            if (ad.get("id").asLong() == adId) {
                return ad.get("attributes");
            }
        }
        throw new IllegalStateException("Ad not found in /mine: " + adId);
    }

    private JsonNode findAttribute(JsonNode attributes, String key) {
        for (JsonNode attribute : attributes) {
            if (attribute.get("key").asText().equals(key)) {
                return attribute;
            }
        }
        return null;
    }

    private String attributeValue(JsonNode attributes, String key) {
        JsonNode attribute = findAttribute(attributes, key);
        return attribute == null ? null : attribute.get("value").asText();
    }

    private String attributeDisplayValue(JsonNode attributes, String key) {
        JsonNode attribute = findAttribute(attributes, key);
        return attribute == null ? null : attribute.get("displayValue").asText();
    }

    private String adBody(String categorySlug, Map<String, String> attributes) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Attribute Test Ad " + UUID.randomUUID());
        body.put("description", "A description long enough for validation.");
        body.put("price", 1000);
        body.put("categorySlug", categorySlug);
        body.put("locationSlug", "colombo");
        body.put("attributes", attributes);
        return objectMapper.writeValueAsString(body);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
