package com.slmanju.ceylonads.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Exercises the real upload -> persist -> read-back path: the local storage profile's
// public-prefix is configured in application-test.yml as "/media", so the generated URL is
// asserted against that prefix rather than a hard-coded value.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class MediaUploadResponseTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Seeding isn't triggered automatically; ensure the "kamal" account and base categories/
    // locations exist regardless of which other test classes have already run in this shared
    // Spring context. LocalDataSeeder.run() is a no-op past account creation once seeded.
    @BeforeEach
    void ensureSeeded() throws Exception {
        seeder.run();
    }

    @Test
    void uploadedMediaUrlIsBuiltFromStorageKeyAndAppearsOnTheAdResponse() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token);

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
        String uploadResponse = mockMvc.perform(multipart("/api/ads/" + adId + "/media")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode uploaded = objectMapper.readTree(uploadResponse);
        long mediaId = uploaded.get("id").asLong();
        String uploadedUrl = uploaded.get("url").asText();
        assertTrue(uploadedUrl.startsWith("/media/"), "expected url under the configured public prefix, got: " + uploadedUrl);

        // storageKey is what's actually persisted; the url above must have been derived from it.
        Media persisted = mediaRepository.findById(mediaId).orElseThrow();
        assertFalse(persisted.getStorageKey().isBlank());
        assertEquals("/media/" + persisted.getStorageKey(), uploadedUrl);

        // Same URL must be reconstructed on every read path, not just the upload response.
        String adResponse = mockMvc.perform(get("/api/ads/" + adId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode media = objectMapper.readTree(adResponse).get("media").get(0);
        assertEquals(uploadedUrl, media.get("url").asText());
    }

    private long createAd(String token) throws Exception {
        Map<String, Object> body = Map.of(
                "title", "Media URL Test Ad " + System.nanoTime(),
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1000"),
                "categorySlug", "services",
                "locationSlug", "colombo",
                "attributes", Map.of("serviceType", "General"));
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
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
