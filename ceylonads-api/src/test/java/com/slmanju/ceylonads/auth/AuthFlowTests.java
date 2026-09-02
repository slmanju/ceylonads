package com.slmanju.ceylonads.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AuthFlowTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerThenLoginReturnsValidJwt() throws Exception {
        String username = "newcustomer-" + UUID.randomUUID();
        String registerBody = """
                {"username":"%s","password":"password123","email":"%s@example.com","displayName":"New Customer","phone":"0770000000"}
                """.formatted(username, username);

        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.username").value(username));

        String loginBody = """
                {"username":"%s","password":"password123"}
                """.formatted(username);

        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void registerDuplicateUsernameIsRejected() throws Exception {
        String username = "dupe-" + UUID.randomUUID();
        String registerBody = """
                {"username":"%s","password":"password123","email":"%s@example.com","displayName":"Dupe","phone":"0770000000"}
                """.formatted(username, username);

        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"kamal\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithValidTokenReturnsProfile() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        mockMvc.perform(get("/api/customers/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("kamal"));
    }

    @Test
    void protectedEndpointWithTamperedTokenIsUnauthorized() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        mockMvc.perform(get("/api/customers/me").header("Authorization", "Bearer " + tampered))
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
