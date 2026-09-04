package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers the public ezClass "Suggest" page endpoint (POST /api/tuition/suggestions, no login
// required) and the ADMIN-only inbox under /api/admin/tuition/suggestions/** (see
// TuitionSuggestionController / AdminTuitionSuggestionController).
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionSuggestionTests {

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
    void publicSubmissionSucceedsWithoutAuthenticationAndLeaksNoId() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Nimal",
                "email", "nimal@example.com",
                "phone", "0712345678",
                "message", "Please add a filter for online-only classes."));

        mockMvc.perform(post("/api/tuition/suggestions").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
    }

    @Test
    void allFieldsExceptMessageAreOptional() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("message", "Just a quick note."));
        mockMvc.perform(post("/api/tuition/suggestions").contentType("application/json").content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void blankMessageIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("message", ""));
        mockMvc.perform(post("/api/tuition/suggestions").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedMessageIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("message", "a".repeat(2001)));
        mockMvc.perform(post("/api/tuition/suggestions").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("message", "Feedback.", "email", "not-an-email"));
        mockMvc.perform(post("/api/tuition/suggestions").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidPhoneIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("message", "Feedback.", "phone", "123"));
        mockMvc.perform(post("/api/tuition/suggestions").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminListingIsUnauthorizedWithoutLoginAndForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/tuition/suggestions")).andExpect(status().isUnauthorized());

        String customerToken = loginAndGetToken("kamal", "customer123");
        mockMvc.perform(get("/api/admin/tuition/suggestions").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        String moderatorToken = loginAndGetToken("moderator1", "moderator123");
        mockMvc.perform(get("/api/admin/tuition/suggestions").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListAndReviewAndCloseASuggestion() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of("message", "Add dark mode please."));
        mockMvc.perform(post("/api/tuition/suggestions").contentType("application/json").content(createBody))
                .andExpect(status().isCreated());

        String adminToken = loginAndGetToken("admin", "admin123");
        String listResponse = mockMvc.perform(get("/api/admin/tuition/suggestions").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(listResponse).get(0).get("id").asLong();

        mockMvc.perform(patch("/api/admin/tuition/suggestions/" + id + "/status")
                        .param("status", "REVIEWED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWED"))
                .andExpect(jsonPath("$.reviewedAt").exists());

        mockMvc.perform(patch("/api/admin/tuition/suggestions/" + id + "/status")
                        .param("status", "CLOSED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
