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
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers global keyword search relevance: ranking order, the "tea" vs "teacher"/"teaching"
 * false-positive regression, case-insensitivity, multi-word AND matching, and composition with
 * the existing category/location filters and pagination.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdSearchRelevanceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Seeding isn't triggered automatically; ensure the "kamal"/"admin" accounts and base
    // categories/locations exist regardless of which other test classes have already run in this
    // shared Spring context. LocalDataSeeder.run() is a no-op past account creation once seeded.
    @BeforeEach
    void ensureSeeded() throws Exception {
        seeder.run();
    }

    @Test
    void teaSearchExcludesUnrelatedTuitionAdButMatchesGenuineTeaAd() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = uniqueMarker("SearchRegression");

        long teaAdId = createApprovedAd(token, marker + " Ceylon Tea Leaves - Wholesale",
                "Premium high-grown Ceylon tea, sold in bulk, long enough for validation.",
                "vehicles", "colombo");
        createApprovedAd(token, marker + " O/L Tuition - Experienced Teacher",
                "Experienced teacher offering weekly teaching sessions, long enough for validation.",
                "vehicles", "colombo");

        mockMvc.perform(get("/api/ads").param("q", "tea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(teaAdId));
    }

    @Test
    void exactTitleMatchRanksAboveWeakerTitleMatch() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = uniqueMarker("ExactTitle");

        long exactId = createApprovedAd(token, marker,
                "A description long enough for validation.", "vehicles", "colombo");
        long looseId = createApprovedAd(token, marker + " with extra words in the title",
                "A description long enough for validation.", "vehicles", "colombo");

        mockMvc.perform(get("/api/ads").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(exactId))
                .andExpect(jsonPath("$.content[1].id").value(looseId));
    }

    @Test
    void titleMatchRanksAboveDescriptionOnlyMatch() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = uniqueMarker("TitleVsDesc");

        long titleMatchId = createApprovedAd(token, marker + " for sale",
                "A description long enough for validation.", "vehicles", "colombo");
        long descriptionOnlyId = createApprovedAd(token, "Unrelated listing title",
                "This item relates to " + marker + " somehow, long enough for validation.",
                "vehicles", "colombo");

        mockMvc.perform(get("/api/ads").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(titleMatchId))
                .andExpect(jsonPath("$.content[1].id").value(descriptionOnlyId));
    }

    @Test
    void searchIsCaseInsensitive() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = uniqueMarker("CaseInsensitive");
        long adId = createApprovedAd(token, marker + " Item",
                "A description long enough for validation.", "vehicles", "colombo");

        mockMvc.perform(get("/api/ads").param("q", marker.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(adId));

        mockMvc.perform(get("/api/ads").param("q", marker.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(adId));
    }

    @Test
    void multiWordSearchFavorsAdsContainingBothTerms() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = uniqueMarker("MultiWord");

        long bothTermsId = createApprovedAd(token, marker + " Toyota Corolla 2016 Hybrid",
                "A description long enough for validation.", "vehicles", "colombo");
        createApprovedAd(token, marker + " Toyota Vitz 2016",
                "A description long enough for validation.", "vehicles", "colombo");
        createApprovedAd(token, marker + " Corolla Shaped Toy Car Accessory",
                "A description long enough for validation.", "vehicles", "colombo");

        mockMvc.perform(get("/api/ads").param("q", marker + " toyota corolla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(bothTermsId));
    }

    @Test
    void searchComposesWithCategoryFilter() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = uniqueMarker("SearchCategory");

        long vehicleId = createApprovedAd(token, marker + " listing",
                "A description long enough for validation.", "vehicles", "colombo");
        createApprovedAd(token, marker + " listing",
                "A description long enough for validation.", "property", "colombo");

        mockMvc.perform(get("/api/ads").param("q", marker).param("category", "vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(vehicleId));
    }

    @Test
    void searchComposesWithLocationFilter() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = uniqueMarker("SearchLocation");

        long colomboId = createApprovedAd(token, marker + " listing",
                "A description long enough for validation.", "vehicles", "colombo");
        createApprovedAd(token, marker + " listing",
                "A description long enough for validation.", "vehicles", "kandy");

        mockMvc.perform(get("/api/ads").param("q", marker).param("location", "colombo-district"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(colomboId));
    }

    @Test
    void paginationAndTotalCountWorkWithKeywordSearch() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String marker = uniqueMarker("SearchPage");

        createApprovedAd(token, marker + " one",
                "A description long enough for validation.", "vehicles", "colombo");
        createApprovedAd(token, marker + " two",
                "A description long enough for validation.", "vehicles", "colombo");
        createApprovedAd(token, marker + " three",
                "A description long enough for validation.", "vehicles", "colombo");

        mockMvc.perform(get("/api/ads").param("q", marker).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/ads").param("q", marker).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    private String uniqueMarker(String label) {
        return label + "Marker" + UUID.randomUUID().toString().replace("-", "");
    }

    private long createApprovedAd(String token, String title, String description, String categorySlug, String locationSlug) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", description,
                "price", new BigDecimal("1000"),
                "categorySlug", categorySlug,
                "locationSlug", locationSlug));
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
