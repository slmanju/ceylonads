package com.slmanju.ceylonads.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.auth.entity.Account;
import com.slmanju.ceylonads.auth.entity.Role;
import com.slmanju.ceylonads.auth.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers the logged-in self-service password change endpoint (PUT /api/account/password). Each
// test creates its own account(s) with unique usernames rather than reusing the shared
// operational/seed accounts, since the H2 database is reused across the whole test JVM run and
// mutating a seed account's password would break unrelated tests that log in as it later.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class ChangePasswordTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void anonymousCallerIsUnauthorized() throws Exception {
        String body = changeBody("old", "newpassword1", "newpassword1");

        mockMvc.perform(put("/api/account/password").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCanChangeOwnPasswordAndLoginWithNewPassword() throws Exception {
        String username = "cp-customer-" + UUID.randomUUID();
        registerCustomer(username, "customer123");
        String token = loginAndGetToken(username, "customer123");

        mockMvc.perform(put("/api/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(changeBody("customer123", "newpassword1", "newpassword1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        assertLoginFails(username, "customer123");
        loginAndGetToken(username, "newpassword1");
    }

    @Test
    void moderatorCanChangeOwnPasswordAndLoginWithNewPassword() throws Exception {
        String username = "cp-moderator-" + UUID.randomUUID();
        accounts.save(new Account(username, passwordEncoder.encode("moderator123"), username + "@example.com", Role.MODERATOR));
        String token = loginAndGetToken(username, "moderator123");

        mockMvc.perform(put("/api/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(changeBody("moderator123", "newpassword1", "newpassword1")))
                .andExpect(status().isOk());

        assertLoginFails(username, "moderator123");
        loginAndGetToken(username, "newpassword1");
    }

    // ADMIN accounts (e.g. the Flyway-seeded manjula/chamila operational accounts) may only have
    // an accounts row with no Customer profile - this account is created the same way to prove
    // the password change does not depend on a Customer relationship.
    @Test
    void adminWithNoCustomerProfileCanChangeOwnPassword() throws Exception {
        String username = "cp-admin-" + UUID.randomUUID();
        accounts.save(new Account(username, passwordEncoder.encode("admin123"), username + "@example.com", Role.ADMIN));
        String token = loginAndGetToken(username, "admin123");

        mockMvc.perform(put("/api/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(changeBody("admin123", "newpassword1", "newpassword1")))
                .andExpect(status().isOk());

        assertLoginFails(username, "admin123");
        loginAndGetToken(username, "newpassword1");
    }

    @Test
    void wrongCurrentPasswordIsRejectedAndPasswordUnchanged() throws Exception {
        String username = "cp-wrongcurrent-" + UUID.randomUUID();
        registerCustomer(username, "customer123");
        String token = loginAndGetToken(username, "customer123");

        mockMvc.perform(put("/api/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(changeBody("wrong-current", "newpassword1", "newpassword1")))
                .andExpect(status().isBadRequest());

        loginAndGetToken(username, "customer123");
    }

    @Test
    void mismatchingNewAndConfirmPasswordIsRejected() throws Exception {
        String username = "cp-mismatch-" + UUID.randomUUID();
        registerCustomer(username, "customer123");
        String token = loginAndGetToken(username, "customer123");

        mockMvc.perform(put("/api/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(changeBody("customer123", "newpassword1", "different1")))
                .andExpect(status().isBadRequest());

        loginAndGetToken(username, "customer123");
    }

    @Test
    void invalidNewPasswordIsRejected() throws Exception {
        String username = "cp-invalid-" + UUID.randomUUID();
        registerCustomer(username, "customer123");
        String token = loginAndGetToken(username, "customer123");

        mockMvc.perform(put("/api/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(changeBody("customer123", "short", "short")))
                .andExpect(status().isBadRequest());

        loginAndGetToken(username, "customer123");
    }

    @Test
    void sameAsCurrentPasswordIsRejected() throws Exception {
        String username = "cp-samepass-" + UUID.randomUUID();
        registerCustomer(username, "customer123");
        String token = loginAndGetToken(username, "customer123");

        mockMvc.perform(put("/api/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(changeBody("customer123", "customer123", "customer123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordHashChangesInDatabaseAndPlainPasswordIsNeverPersisted() throws Exception {
        String username = "cp-hash-" + UUID.randomUUID();
        registerCustomer(username, "customer123");
        String beforeHash = accounts.findByUsernameIgnoreCase(username).orElseThrow().getPasswordHash();
        String token = loginAndGetToken(username, "customer123");

        mockMvc.perform(put("/api/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(changeBody("customer123", "newpassword1", "newpassword1")))
                .andExpect(status().isOk());

        String afterHash = accounts.findByUsernameIgnoreCase(username).orElseThrow().getPasswordHash();
        org.junit.jupiter.api.Assertions.assertNotEquals(beforeHash, afterHash);
        org.junit.jupiter.api.Assertions.assertNotEquals("newpassword1", afterHash);
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("newpassword1", afterHash));
    }

    @Test
    void userCannotChangeAnotherUsersPassword() throws Exception {
        String usernameA = "cp-usera-" + UUID.randomUUID();
        String usernameB = "cp-userb-" + UUID.randomUUID();
        registerCustomer(usernameA, "customer123");
        registerCustomer(usernameB, "customer123");
        String tokenB = loginAndGetToken(usernameB, "customer123");

        // The request has no field for identifying the target account - authorization comes
        // solely from B's own token - so B changing "their" password can only ever affect B.
        mockMvc.perform(put("/api/account/password")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType("application/json")
                        .content(changeBody("customer123", "newpassword1", "newpassword1")))
                .andExpect(status().isOk());

        // A's original password still works; A was untouched.
        loginAndGetToken(usernameA, "customer123");
    }

    private void registerCustomer(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s","email":"%s@example.com","displayName":"Test User","phone":"0770000000"}
                """.formatted(username, password, username);

        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());
    }

    private String changeBody(String currentPassword, String newPassword, String confirmPassword) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "currentPassword", currentPassword,
                "newPassword", newPassword,
                "confirmPassword", confirmPassword));
    }

    private void assertLoginFails(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
