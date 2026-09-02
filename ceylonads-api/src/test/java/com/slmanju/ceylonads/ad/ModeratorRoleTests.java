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

// Covers the MODERATOR role: it can create/manage its own ads and moderate ads (including its
// own, per the MVP self-approval allowance), but must never reach promotion/payment
// administration - that stays ADMIN-only. See CLAUDE.md's "Roles and authentication" section.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class ModeratorRoleTests {

    private static final String NEW_AD_BODY = """
            {"title":"Test Ad %s","description":"A description long enough for validation.","price":1000,\
            "categorySlug":"vehicles","locationSlug":"colombo"}
            """;

    // categorySlug "vehicles" (a top-level category with no required attributes), same as
    // NEW_AD_BODY - "services" would need its required serviceType attribute filled in too.
    private static final String CONTRACTOR_AD_BODY = """
            {"title":"Electrical Repairs by ABC","description":"Contractor listing posted on behalf of a local business.",\
            "price":5000,"categorySlug":"vehicles","locationSlug":"colombo",\
            "contactName":"ABC Electrical Services","phoneNumber":"0712345678"}
            """;

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
    void moderatorCanAuthenticateWithModeratorAuthority() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("username", "moderator1", "password", "moderator123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MODERATOR"));
    }

    @Test
    void moderatorCanAccessModerationQueueAndAdminCanToo() throws Exception {
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");
        String adminToken = loginAndGetToken("admin", "admin123");

        mockMvc.perform(get("/api/moderation/ads/pending").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/moderation/ads/pending").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void moderatorCanCreateOwnAdWithContractorContactDetails() throws Exception {
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");

        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType("application/json")
                        .content(CONTRACTOR_AD_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.contactOverride.contactName").value("ABC Electrical Services"))
                .andExpect(jsonPath("$.contactOverride.phoneNumber").value("0712345678"))
                .andReturn().getResponse().getContentAsString();
        long adId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/ads/mine").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + adId + ")]").exists());
    }

    @Test
    void moderatorCanApproveOwnCreatedAdAndPublicContactShowsContractor() throws Exception {
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");
        long adId = createAd(moderatorToken, CONTRACTOR_AD_BODY);

        // Not public yet.
        mockMvc.perform(get("/api/ads/" + adId)).andExpect(status().isNotFound());

        // Self-approval: same account created and approves the ad.
        mockMvc.perform(patch("/api/moderation/ads/" + adId + "/approve").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.reviewedAt").exists());

        mockMvc.perform(get("/api/ads/" + adId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.name").value("ABC Electrical Services"))
                .andExpect(jsonPath("$.contact.phoneNumber").value("0712345678"));
    }

    @Test
    void moderatorCanApproveCustomerCreatedAd() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createAd(kamalToken, NEW_AD_BODY.formatted("by-customer"));

        String moderatorToken = loginAndGetToken("moderator1", "moderator123");
        mockMvc.perform(patch("/api/moderation/ads/" + adId + "/approve").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/ads/" + adId)).andExpect(status().isOk());
    }

    @Test
    void moderatorCanRejectAd() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createAd(kamalToken, NEW_AD_BODY.formatted("to-reject"));

        String moderatorToken = loginAndGetToken("moderator1", "moderator123");
        mockMvc.perform(patch("/api/moderation/ads/" + adId + "/reject").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void moderatorCannotEditAnotherAccountsAdOutsideModeration() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long adId = createAd(kamalToken, NEW_AD_BODY.formatted("owned-by-kamal"));

        String moderatorToken = loginAndGetToken("moderator1", "moderator123");
        mockMvc.perform(put("/api/ads/" + adId)
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType("application/json")
                        .content(NEW_AD_BODY.formatted("hijacked-by-moderator")))
                .andExpect(status().isForbidden());
    }

    @Test
    void moderatorCannotApproveOrManagePromotions() throws Exception {
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");

        mockMvc.perform(patch("/api/admin/promotions/1/approve").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/promotions/1/cancel").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/promotions").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void moderatorCannotVerifyOrAccessPayments() throws Exception {
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");

        mockMvc.perform(post("/api/admin/payments/1/approve")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/payments/1/reject")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType("application/json")
                        .content("{\"reason\":\"not a real payment\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/payments/1").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/payments").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminIsUnaffectedByModerationBoundary() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");

        mockMvc.perform(get("/api/admin/promotions").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/payments").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/moderation/ads/pending").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private long createAd(String token, String body) throws Exception {
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
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
