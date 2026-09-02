package com.slmanju.ceylonads.ad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the hierarchical category/location expansion and the new search validation behavior
 * (unknown slugs, invalid price range, unsupported/invalid attribute filters) added on top of the
 * existing GET /api/ads search endpoint.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdSearchHierarchyTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void childCategoryMatchesOnlyThatCategory() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "ChildCatMarker-" + UUID.randomUUID();

        long carId = createApprovedAd(token, marker + " car", "cars", "colombo", carAttrs());
        createApprovedAd(token, marker + " bike", "motorcycles", "colombo", motorcycleAttrs());

        mockMvc.perform(get("/api/ads").param("q", marker).param("category", "cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(carId));
    }

    @Test
    void parentCategoryIncludesAllDescendantCategories() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "ParentCatMarker-" + UUID.randomUUID();

        createApprovedAd(token, marker + " car", "cars", "colombo", carAttrs());
        createApprovedAd(token, marker + " bike", "motorcycles", "colombo", motorcycleAttrs());

        mockMvc.perform(get("/api/ads").param("q", marker).param("category", "vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void noLocationMatchesAdsFromAnywhereInSriLanka() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "NoLocationMarker-" + UUID.randomUUID();

        createApprovedAd(token, marker + " colombo", "cars", "colombo", carAttrs());
        createApprovedAd(token, marker + " kandy", "cars", "kandy", carAttrs());

        mockMvc.perform(get("/api/ads").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void parentLocationIncludesDescendantLocationsButNotOtherBranches() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "ParentLocMarker-" + UUID.randomUUID();

        long colomboId = createApprovedAd(token, marker + " colombo", "cars", "colombo", carAttrs());
        createApprovedAd(token, marker + " kandy", "cars", "kandy", carAttrs());

        mockMvc.perform(get("/api/ads").param("q", marker).param("location", "colombo-district"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(colomboId));

        mockMvc.perform(get("/api/ads").param("q", marker).param("location", "western-province"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(colomboId));

        mockMvc.perform(get("/api/ads").param("q", marker).param("location", "central-province"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value(marker + " kandy"));
    }

    @Test
    void categoryAndLocationFiltersComposeTogether() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "ComboCatLocMarker-" + UUID.randomUUID();

        // "wrongCategory" uses Houses (under Property), which is outside the Vehicles branch
        // entirely - unlike Motorcycles, which would also satisfy category=vehicles.
        long matchId = createApprovedAd(token, marker + " match", "cars", "colombo", carAttrs());
        createApprovedAd(token, marker + " wrongLocation", "cars", "kandy", carAttrs());
        createApprovedAd(token, marker + " wrongCategory", "houses", "colombo", houseAttrs());

        mockMvc.perform(get("/api/ads").param("q", marker).param("category", "vehicles").param("location", "western-province"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matchId));
    }

    @Test
    void unknownCategorySlugIsNotFound() throws Exception {
        mockMvc.perform(get("/api/ads").param("category", "not-a-real-category"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownLocationSlugIsNotFound() throws Exception {
        mockMvc.perform(get("/api/ads").param("location", "not-a-real-location"))
                .andExpect(status().isNotFound());
    }

    @Test
    void minPriceGreaterThanMaxPriceIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/ads").param("minPrice", "5000").param("maxPrice", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedAttributeFilterKeyIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/ads").param("attr.notARealAttribute", "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSelectAttributeFilterValueIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/ads").param("attr.fuelType", "NOT_A_FUEL_TYPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidNumericAttributeFilterValueIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/ads").param("attr.year", "not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidBooleanAttributeFilterValueIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/ads").param("attr.furnished", "maybe"))
                .andExpect(status().isBadRequest());
    }

    private Map<String, String> carAttrs() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("make", "Toyota");
        attrs.put("model", "Aqua");
        attrs.put("year", "2020");
        attrs.put("mileage", "50000");
        attrs.put("fuelType", "HYBRID");
        attrs.put("transmission", "AUTOMATIC");
        return attrs;
    }

    private Map<String, String> houseAttrs() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("propertyType", "HOUSE");
        attrs.put("bedrooms", "3");
        attrs.put("bathrooms", "2");
        return attrs;
    }

    private Map<String, String> motorcycleAttrs() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("make", "Honda");
        attrs.put("model", "CB125");
        attrs.put("year", "2019");
        attrs.put("mileage", "12000");
        attrs.put("fuelType", "PETROL");
        attrs.put("transmission", "MANUAL");
        return attrs;
    }

    private long createApprovedAd(String token, String title, String categorySlug, String locationSlug, Map<String, String> attributes) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", "A description long enough for validation.");
        body.put("price", new BigDecimal("1000"));
        body.put("categorySlug", categorySlug);
        body.put("locationSlug", locationSlug);
        body.put("attributes", attributes);

        String response = mockMvc.perform(post("/api/ads")
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

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
