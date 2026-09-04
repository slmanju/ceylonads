package com.slmanju.ceylonads.ad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Regression coverage for the Tuition Admin rollout accidentally narrowing ADMIN out of ordinary
// self-service capability (My Ads/My Classes, via /api/ads/mine and /api/tuition/my-classes) -
// ADMIN is additive on top of CUSTOMER/MODERATOR, never a replacement for it. See
// AdController/TuitionMyClassesController/TuitionClassController's hasAnyRole(...) lists and
// ceylonads-tuition-ui's ProtectedRoute (isAuthenticated-only) / ProtectedAdminRoute (ADMIN-only).
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdminSelfServiceAccessTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void adminCanUseSelfServiceMineEndpointsLikeAnyOtherAuthenticatedAccount() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");

        mockMvc.perform(get("/api/ads/mine").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tuition/my-classes").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanPostAndSeeItsOwnTuitionClassThroughMineEndpoints() throws Exception {
        String username = "admin_selfservice_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        registerCustomer(username);
        // Role escalation has no self-service or admin API (intentionally - see
        // AdminService/AdminController); every other cross-role fixture in this test suite
        // promotes directly at the persistence layer the same way.
        jdbcTemplate.update("update accounts set role = 'ADMIN' where lower(username) = lower(?)", username);
        String adminToken = loginAndGetToken(username, "customer123");

        String body = """
                {"title":"Admin Owned Class","description":"A description long enough for validation purposes.",\
                "price":2000,"categorySlug":"school-tuition","locationSlugs":["colombo"],"subject":"Physics",\
                "level":"AL","curriculum":"LOCAL","medium":["ENGLISH"],"deliveryMode":"PHYSICAL","classFormat":"INDIVIDUAL"}
                """;
        mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        mockMvc.perform(get("/api/ads/mine").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Admin Owned Class')]").exists());
        mockMvc.perform(get("/api/tuition/my-classes").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Admin Owned Class')]").exists());
    }

    @Test
    void customerAndModeratorSelfServiceRemainUnaffectedAndStillDeniedTuitionAdmin() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");

        mockMvc.perform(get("/api/ads/mine").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/ads/mine").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/tuition/pending").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/tuition/pending").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
    }

    private void registerCustomer(String username) throws Exception {
        Map<String, Object> body = Map.of(
                "username", username,
                "password", "customer123",
                "email", username + "@example.test",
                "displayName", "Self Service Test");
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
