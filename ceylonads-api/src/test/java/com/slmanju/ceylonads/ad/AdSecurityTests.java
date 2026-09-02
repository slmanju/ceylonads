package com.slmanju.ceylonads.ad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdSecurityTests {

    private static final String NEW_AD_BODY = """
            {"title":"Test Ad %s","description":"A description long enough for validation.","price":1000,"categorySlug":"vehicles","locationSlug":"colombo"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Guards against relying on another test class having already seeded the shared in-memory
    // database first - run() is idempotent, so this is safe even if seeding already happened.
    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void anonymousCanBrowsePublicListings() throws Exception {
        mockMvc.perform(get("/api/ads")).andExpect(status().isOk());
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/api/locations")).andExpect(status().isOk());
    }

    @Test
    void customerCanCreateUpdateAndDeactivateOwnAd() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");

        long adId = createAd(kamalToken, "own");

        mockMvc.perform(put("/api/ads/" + adId)
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(NEW_AD_BODY.formatted("own-updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Ad own-updated"));

        mockMvc.perform(get("/api/ads/mine").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")]").exists());

        mockMvc.perform(delete("/api/ads/" + adId).header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void customerCannotModifyAnotherCustomersAd() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String nimalToken = loginAndGetToken("nimal", "customer123");

        long adId = createAd(kamalToken, "guarded");

        mockMvc.perform(put("/api/ads/" + adId)
                        .header("Authorization", "Bearer " + nimalToken)
                        .contentType("application/json")
                        .content(NEW_AD_BODY.formatted("hijacked")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/ads/" + adId).header("Authorization", "Bearer " + nimalToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotAccessAdminEndpoints() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");

        mockMvc.perform(get("/api/admin/ads/pending").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotAccessModerationQueue() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");

        mockMvc.perform(get("/api/moderation/ads/pending").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanModeratePendingAds() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createAd(kamalToken, "moderation");

        String adminToken = loginAndGetToken("admin", "admin123");

        mockMvc.perform(get("/api/admin/ads/pending").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")]").exists());

        // Not yet visible publicly while pending review.
        mockMvc.perform(get("/api/ads/" + adId)).andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/admin/ads/" + adId + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/ads/" + adId)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/ads/" + adId + "/deactivate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEACTIVATED"));

        mockMvc.perform(get("/api/ads/" + adId)).andExpect(status().isNotFound());
    }

    private long createAd(String token, String label) throws Exception {
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(NEW_AD_BODY.formatted(label)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
