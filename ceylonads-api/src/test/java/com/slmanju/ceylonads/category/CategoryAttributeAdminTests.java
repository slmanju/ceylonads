package com.slmanju.ceylonads.category;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class CategoryAttributeAdminTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void adminCanCreateUpdateDeactivateAttributeAndManageOptions() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long categoryId = categoryIdBySlug(adminToken, "cars");
        String key = "testAttr" + System.currentTimeMillis();

        String createBody = objectMapper.writeValueAsString(Map.of(
                "key", key, "name", "Test Attribute", "dataType", "SELECT",
                "required", false, "filterable", true, "searchable", false, "displayOrder", 99,
                "options", java.util.List.of(Map.of("value", "A", "label", "Option A", "displayOrder", 1))));

        String createResponse = mockMvc.perform(post("/api/admin/categories/" + categoryId + "/attributes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.dataType").value("SELECT"))
                .andExpect(jsonPath("$.options.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        long attributeId = objectMapper.readTree(createResponse).get("id").asLong();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "name", "Renamed Attribute", "required", true, "filterable", false, "searchable", false,
                "displayOrder", 5, "active", true));
        mockMvc.perform(put("/api/admin/categories/" + categoryId + "/attributes/" + attributeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Attribute"))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.dataType").value("SELECT"));

        String optionBody = objectMapper.writeValueAsString(Map.of("value", "B", "label", "Option B", "displayOrder", 2));
        String optionResponse = mockMvc.perform(post("/api/admin/categories/" + categoryId + "/attributes/" + attributeId + "/options")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(optionBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value("B"))
                .andReturn().getResponse().getContentAsString();
        long optionId = objectMapper.readTree(optionResponse).get("id").asLong();

        mockMvc.perform(patch("/api/admin/categories/" + categoryId + "/attributes/" + attributeId + "/options/" + optionId + "/active")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/admin/categories/" + categoryId + "/attributes/" + attributeId + "/active")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/admin/categories/" + categoryId + "/attributes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == '" + key + "')].active").value(false));
    }

    @Test
    void duplicateKeyWithinSameCategoryIsRejected() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long categoryId = categoryIdBySlug(adminToken, "houses");

        String body = objectMapper.writeValueAsString(Map.of(
                "key", "bedrooms", "name", "Bedrooms Duplicate", "dataType", "NUMBER",
                "required", false, "filterable", false, "searchable", false, "displayOrder", 1));

        mockMvc.perform(post("/api/admin/categories/" + categoryId + "/attributes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void selectAttributeWithoutOptionsIsRejected() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long categoryId = categoryIdBySlug(adminToken, "cars");

        String body = objectMapper.writeValueAsString(Map.of(
                "key", "noOptions" + System.currentTimeMillis(), "name", "No Options", "dataType", "SELECT",
                "required", false, "filterable", false, "searchable", false, "displayOrder", 1));

        mockMvc.perform(post("/api/admin/categories/" + categoryId + "/attributes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminCannotManageAttributeDefinitions() throws Exception {
        String customerToken = loginAndGetToken("kamal", "customer123");
        long categoryId = categoryIdBySlug(customerToken, "cars");

        mockMvc.perform(get("/api/admin/categories/" + categoryId + "/attributes")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/categories/" + categoryId + "/attributes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointReturnsOnlyActiveAttributesWithOptions() throws Exception {
        mockMvc.perform(get("/api/categories/cars/attributes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'make')].dataType").value("SELECT"))
                .andExpect(jsonPath("$[?(@.key == 'make')].options[0].value").exists())
                .andExpect(jsonPath("$[?(@.key == 'fuelType')].filterable").value(true));
    }

    private long categoryIdBySlug(String token, String slug) throws Exception {
        String response = mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("slug").asText().equals(slug)) {
                return node.get("id").asLong();
            }
        }
        throw new IllegalStateException("Seed category not found: " + slug);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
