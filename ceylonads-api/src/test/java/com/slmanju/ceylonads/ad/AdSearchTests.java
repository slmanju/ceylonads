package com.slmanju.ceylonads.ad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdSearchTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void defaultPaginationReturnsPageResponseEnvelope() throws Exception {
        mockMvc.perform(get("/api/ads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void customPageSizeLimitsResultsAndReportsMetadata() throws Exception {
        mockMvc.perform(get("/api/ads").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void priceAscendingAndDescendingSortOrderResults() throws Exception {
        String marker = "PriceSortMarker-" + UUID.randomUUID();
        String token = loginAndGetToken("kamal", "customer123");
        long cheapId = createApprovedAd(token, marker + " cheap", new BigDecimal("1000"));
        long expensiveId = createApprovedAd(token, marker + " expensive", new BigDecimal("999999"));

        mockMvc.perform(get("/api/ads").param("q", marker).param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(cheapId))
                .andExpect(jsonPath("$.content[1].id").value(expensiveId));

        mockMvc.perform(get("/api/ads").param("q", marker).param("sort", "price_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(expensiveId))
                .andExpect(jsonPath("$.content[1].id").value(cheapId));
    }

    @Test
    void newestSortingIsDefaultAndOrdersByCreationRecency() throws Exception {
        String marker = "NewestSortMarker-" + UUID.randomUUID();
        String token = loginAndGetToken("kamal", "customer123");
        long firstId = createApprovedAd(token, marker + " first", new BigDecimal("5000"));
        Thread.sleep(5);
        long secondId = createApprovedAd(token, marker + " second", new BigDecimal("5000"));

        mockMvc.perform(get("/api/ads").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(secondId))
                .andExpect(jsonPath("$.content[1].id").value(firstId));
    }

    @Test
    void unknownSortValueFallsBackToNewest() throws Exception {
        mockMvc.perform(get("/api/ads").param("sort", "not-a-real-sort"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void filterAndPaginationCompose() throws Exception {
        String marker = "FilterPageMarker-" + UUID.randomUUID();
        String token = loginAndGetToken("kamal", "customer123");
        createApprovedAd(token, marker + " one", new BigDecimal("1000"));
        createApprovedAd(token, marker + " two", new BigDecimal("2000"));
        createApprovedAd(token, marker + " three", new BigDecimal("3000"));

        mockMvc.perform(get("/api/ads")
                        .param("q", marker)
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value(marker + " three"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void sellerDtoIncludesIdDisplayNameAndPhoneForActivePublicAd() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createApprovedAd(token, "Seller DTO Test " + UUID.randomUUID(), new BigDecimal("1000"));

        mockMvc.perform(get("/api/ads/" + adId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seller.id").isNumber())
                .andExpect(jsonPath("$.seller.displayName").value("Kamal Perera"))
                .andExpect(jsonPath("$.seller.phone").value("0771234567"));
    }

    private long createApprovedAd(String token, String title, BigDecimal price) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "A description long enough for validation.",
                "price", price,
                "categorySlug", "vehicles",
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
