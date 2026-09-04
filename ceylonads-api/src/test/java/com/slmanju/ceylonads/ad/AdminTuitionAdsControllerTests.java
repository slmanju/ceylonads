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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers /api/admin/tuition/** (see AdminTuitionAdsController): ADMIN-only, always scoped to
// SourceChannel.TUITION regardless of an id from another channel, reusing AdService's existing
// approve/reject/pendingReview/getForAdmin channel guard - never a duplicate moderation
// implementation.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdminTuitionAdsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void pendingListOnlyContainsTuitionClasses() throws Exception {
        String token = registerAndGetToken();
        long tuitionId = createTuitionClass(token, tuitionBody("Pending Tuition " + UUID.randomUUID()));
        long mainSiteId = createMainSiteAd(token, "Pending Main Site Ad " + UUID.randomUUID());

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(get("/api/admin/tuition/pending").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + tuitionId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + mainSiteId + ")]").doesNotExist());
    }

    @Test
    void approvingThroughTuitionAdminAppliesFreeListingExpiry() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Approve Via Tuition Admin " + UUID.randomUUID()));

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/tuition/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.publishedAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists());

        mockMvc.perform(get("/api/admin/tuition/pending").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").doesNotExist());
    }

    @Test
    void rejectingThroughTuitionAdminWorksAndOwnerStillSeesIt() throws Exception {
        String token = registerAndGetToken();
        long id = createTuitionClass(token, tuitionBody("Reject Via Tuition Admin " + UUID.randomUUID()));

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/tuition/ads/" + id + "/reject").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/tuition/my-classes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
    }

    @Test
    void mainSiteAdCannotBeApprovedOrRejectedThroughTuitionAdmin() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long mainSiteAdId = createMainSiteAd(kamalToken, "Main Site Ad " + UUID.randomUUID());

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(get("/api/admin/tuition/ads/" + mainSiteAdId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/admin/tuition/ads/" + mainSiteAdId + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/admin/tuition/ads/" + mainSiteAdId + "/reject").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminCannotReachTuitionAdminEndpoints() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");

        mockMvc.perform(get("/api/admin/tuition/pending").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/tuition/pending").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/tuition/dashboard").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardReturnsChannelScopedCounts() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(get("/api/admin/tuition/dashboard").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingClasses").isNumber())
                .andExpect(jsonPath("$.activeClasses").isNumber())
                .andExpect(jsonPath("$.expiredClasses").isNumber())
                .andExpect(jsonPath("$.newSuggestions").isNumber())
                .andExpect(jsonPath("$.pendingPromotions").isNumber())
                .andExpect(jsonPath("$.activePromotions").isNumber())
                .andExpect(jsonPath("$.currentPromotionPlans").isNumber())
                .andExpect(jsonPath("$.currentCampaigns").isNumber());
    }

    private Map<String, Object> tuitionBody(String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "A description long enough for validation purposes.");
        body.put("price", 3000);
        body.put("categorySlug", "school-tuition");
        body.put("locationSlugs", List.of("colombo"));
        body.put("subject", "Combined Mathematics");
        body.put("level", "A/L");
        body.put("curriculum", "LOCAL");
        body.put("medium", List.of("ENGLISH"));
        body.put("deliveryMode", "PHYSICAL");
        body.put("classFormat", "INDIVIDUAL");
        return body;
    }

    private long createTuitionClass(String token, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createMainSiteAd(String token, String title) throws Exception {
        String body = """
                {"title":"%s","description":"A description long enough for validation.","price":1000,\
                "categorySlug":"vehicles","locationSlug":"colombo"}
                """.formatted(title);
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String registerAndGetToken() throws Exception {
        String username = "tuition_admin_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        Map<String, Object> body = Map.of(
                "username", username,
                "password", "customer123",
                "email", username + "@example.test",
                "displayName", "Admin Test Tutor");
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
