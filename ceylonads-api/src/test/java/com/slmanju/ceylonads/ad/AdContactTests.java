package com.slmanju.ceylonads.ad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// kamal's seeded account contact (see LocalDataSeeder): displayName "Kamal Perera", phone
// "0771234567" - used throughout as the expected fallback.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdContactTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Seeds explicitly rather than relying on data already being present, matching
    // AdQueryCountTests - nothing in this test module seeds automatically on startup.
    @BeforeEach
    void seed() throws Exception {
        seeder.run();
    }

    @Test
    void adWithNoContactOverrideResolvesToAccountContact() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token, Map.of());
        approve(adId);

        mockMvc.perform(get("/api/ads/" + adId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.name").value("Kamal Perera"))
                .andExpect(jsonPath("$.contact.phoneNumber").value("0771234567"))
                .andExpect(jsonPath("$.contact.whatsappNumber").value("0771234567"));
    }

    @Test
    void adSpecificPhoneOverridesAccountPhone() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token, Map.of("phoneNumber", "0712223344"));
        approve(adId);

        mockMvc.perform(get("/api/ads/" + adId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.phoneNumber").value("0712223344"))
                // WhatsApp wasn't overridden, so it still falls back to the account phone.
                .andExpect(jsonPath("$.contact.whatsappNumber").value("0771234567"));
    }

    @Test
    void adSpecificWhatsappOverridesFallback() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token, Map.of("whatsappNumber", "+94719998877"));
        approve(adId);

        mockMvc.perform(get("/api/ads/" + adId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.whatsappNumber").value("+94719998877"))
                .andExpect(jsonPath("$.contact.phoneNumber").value("0771234567"));
    }

    @Test
    void adSpecificContactNameOverridesSellerDisplayName() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token, Map.of("contactName", "Nimal - Technician"));
        approve(adId);

        mockMvc.perform(get("/api/ads/" + adId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.name").value("Nimal - Technician"));
    }

    @Test
    void clearingOverrideRestoresAccountFallback() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token, Map.of("phoneNumber", "0712223344"));

        // Save again with a blank phone - this must clear the override, not persist "" forever.
        mockMvc.perform(put("/api/ads/" + adId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(adBody(Map.of("phoneNumber", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactOverride.phoneNumber").doesNotExist());

        approve(adId);
        mockMvc.perform(get("/api/ads/" + adId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.phoneNumber").value("0771234567"));
    }

    @Test
    void invalidSuppliedPhoneIsRejected() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");

        mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(adBody(Map.of("phoneNumber", "not-a-phone"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerCannotEditAnotherUsersAdContact() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String nimalToken = loginAndGetToken("nimal", "customer123");

        long adId = createAd(kamalToken, Map.of("phoneNumber", "0712223344"));

        mockMvc.perform(put("/api/ads/" + adId)
                        .header("Authorization", "Bearer " + nimalToken)
                        .contentType("application/json")
                        .content(adBody(Map.of("phoneNumber", "0799999999"))))
                .andExpect(status().isForbidden());

        // Confirm the override wasn't touched by the rejected request.
        String mineResponse = mockMvc.perform(get("/api/ads/mine").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode mine = objectMapper.readTree(mineResponse);
        for (JsonNode ad : mine) {
            if (ad.get("id").asLong() == adId) {
                org.junit.jupiter.api.Assertions.assertEquals("0712223344", ad.get("contactOverride").get("phoneNumber").asText());
                return;
            }
        }
        org.junit.jupiter.api.Assertions.fail("Ad not found in kamal's own list");
    }

    @Test
    void existingLegacyAdWithNullContactColumnsStillWorks() throws Exception {
        // Seeded ads predate this feature and never set contact overrides - assert one still
        // resolves cleanly to the account contact instead of erroring.
        String response = mockMvc.perform(get("/api/ads").param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long adId = objectMapper.readTree(response).get("content").get(0).get("id").asLong();

        mockMvc.perform(get("/api/ads/" + adId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.name").exists());
    }

    @Test
    void publicDetailResponseExposesResolvedContactButNoPrivateAccountData() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token, Map.of("contactName", "ABC Electrical Services", "phoneNumber", "0712223344"));
        approve(adId);

        mockMvc.perform(get("/api/ads/" + adId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.name").value("ABC Electrical Services"))
                .andExpect(jsonPath("$.contact.phoneNumber").value("0712223344"))
                // The raw per-ad override and the seller's account id/email are owner-only data,
                // not part of the public detail response.
                .andExpect(jsonPath("$.contactOverride").doesNotExist())
                .andExpect(jsonPath("$.seller.email").doesNotExist());
    }

    private void approve(long adId) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + adId + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private long createAd(String token, Map<String, Object> contactOverrides) throws Exception {
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(adBody(contactOverrides)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String adBody(Map<String, Object> contactOverrides) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Contact Test Ad " + UUID.randomUUID());
        body.put("description", "A description long enough for validation.");
        body.put("price", 1000);
        body.put("categorySlug", "vehicles");
        body.put("locationSlug", "colombo");
        body.putAll(contactOverrides);
        return objectMapper.writeValueAsString(body);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
