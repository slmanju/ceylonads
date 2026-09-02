package com.slmanju.ceylonads.ad;

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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdAttributeFilterTests {

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

    @Test
    void filterBySelectAttributeReturnsOnlyMatchingAds() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "SelectFilterMarker-" + UUID.randomUUID();

        long hybridId = createApprovedCar(token, marker + " hybrid", carAttrs("HYBRID", "2020"));
        long petrolId = createApprovedCar(token, marker + " petrol", carAttrs("PETROL", "2020"));

        mockMvc.perform(get("/api/ads").param("q", marker).param("attr.fuelType", "HYBRID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(hybridId));

        // sanity check both ads exist unfiltered
        mockMvc.perform(get("/api/ads").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        org.junit.jupiter.api.Assertions.assertNotEquals(hybridId, petrolId);
    }

    @Test
    void filterByNumericAttributeExactValueMatches() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "ExactYearMarker-" + UUID.randomUUID();

        long matchId = createApprovedCar(token, marker + " match", carAttrs("HYBRID", "2018"));
        createApprovedCar(token, marker + " other", carAttrs("HYBRID", "2022"));

        mockMvc.perform(get("/api/ads").param("q", marker).param("attr.year", "2018"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matchId));
    }

    @Test
    void numericRangeFilteringNarrowsResults() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "RangeMarker-" + UUID.randomUUID();

        long inRangeId = createApprovedCar(token, marker + " inrange", carAttrs("HYBRID", "2020"));
        createApprovedCar(token, marker + " tooOld", carAttrs("HYBRID", "2010"));
        createApprovedCar(token, marker + " tooNew", carAttrs("HYBRID", "2025"));

        mockMvc.perform(get("/api/ads").param("q", marker)
                        .param("attr.year.min", "2015").param("attr.year.max", "2021"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(inRangeId));
    }

    @Test
    void categoryAndAttributeFiltersComposeTogether() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "ComboMarker-" + UUID.randomUUID();

        long carId = createApprovedCar(token, marker + " car", carAttrs("HYBRID", "2020"));
        // Same fuel-type value happens to not exist outside "cars", but scope by category anyway.
        mockMvc.perform(get("/api/ads")
                        .param("q", marker)
                        .param("category", "cars")
                        .param("attr.fuelType", "HYBRID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(carId));

        mockMvc.perform(get("/api/ads")
                        .param("q", marker)
                        .param("category", "houses")
                        .param("attr.fuelType", "HYBRID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void paginationStillWorksWithAttributeFilters() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = "PagedFilterMarker-" + UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            createApprovedCar(token, marker + " " + i, carAttrs("DIESEL", "2020"));
        }

        mockMvc.perform(get("/api/ads").param("q", marker).param("attr.fuelType", "DIESEL")
                        .param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    private Map<String, String> carAttrs(String fuelType, String year) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("make", "Toyota");
        attrs.put("model", "Aqua");
        attrs.put("year", year);
        attrs.put("mileage", "50000");
        attrs.put("fuelType", fuelType);
        attrs.put("transmission", "AUTOMATIC");
        return attrs;
    }

    private long createApprovedCar(String token, String title, Map<String, String> attributes) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", "A description long enough for validation.");
        body.put("price", new BigDecimal("1000"));
        body.put("categorySlug", "cars");
        body.put("locationSlug", "colombo");
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
