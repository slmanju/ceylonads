package com.slmanju.ceylonads.tuition;

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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises GET /api/tuition/classes/{slug}. Fixture: LocalDataSeeder's "English Classes for
 * Kids - Ratnapura" tuition ad (price 2200, unique among seeded tuition ads, so it's locatable via
 * a plain price filter without depending on generated ids or seed ordering) - subject=English,
 * grade=Primary, curriculum=LOCAL, medium=[ENGLISH], classMode=PHYSICAL, classType=GROUP,
 * location=ratnapura.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionClassDetailTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seed() throws Exception {
        seeder.run();
    }

    @Test
    void validTuitionDetailIsMappedFully() throws Exception {
        long adId = englishClassesForKidsAdId();

        mockMvc.perform(get("/api/tuition/classes/" + adId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adId))
                .andExpect(jsonPath("$.slug").value(org.hamcrest.Matchers.endsWith("-" + adId)))
                .andExpect(jsonPath("$.title").value("English Classes for Kids - Ratnapura"))
                .andExpect(jsonPath("$.price").value(2200))
                .andExpect(jsonPath("$.categorySlug").value("school-tuition"))
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.publishedAt").isNotEmpty())
                // Academic info: TEXT attributes mapped as plain strings, SELECT/MULTI_SELECT as
                // value+label pairs.
                .andExpect(jsonPath("$.academic.subject").value("English"))
                .andExpect(jsonPath("$.academic.level").value("Primary"))
                .andExpect(jsonPath("$.academic.curriculum.value").value("LOCAL"))
                .andExpect(jsonPath("$.academic.curriculum.label").value("Local"))
                .andExpect(jsonPath("$.academic.medium[0].value").value("ENGLISH"))
                .andExpect(jsonPath("$.academic.medium[0].label").value("English"))
                .andExpect(jsonPath("$.academic.medium.length()").value(1))
                // Class info.
                .andExpect(jsonPath("$.classInfo.deliveryModes[0].value").value("PHYSICAL"))
                .andExpect(jsonPath("$.classInfo.deliveryModes[0].label").value("Physical"))
                .andExpect(jsonPath("$.classInfo.classFormats[0].value").value("GROUP"))
                .andExpect(jsonPath("$.classInfo.classFormats[0].label").value("Group"))
                // No attribute currently backs classPurposes - must come back as a clean empty list.
                .andExpect(jsonPath("$.classInfo.classPurposes").isArray())
                .andExpect(jsonPath("$.classInfo.classPurposes.length()").value(0))
                // Locations.
                .andExpect(jsonPath("$.locations[0].slug").value("ratnapura"))
                .andExpect(jsonPath("$.locations[0].name").isNotEmpty())
                // Media: display order present (may be empty for this fixture, but must be an array).
                .andExpect(jsonPath("$.media").isArray())
                // Contact.
                .andExpect(jsonPath("$.contact.name").isNotEmpty())
                .andExpect(jsonPath("$.contact.phoneNumber").isNotEmpty());
    }

    @Test
    void nonTuitionAdReturnsNotFound() throws Exception {
        long carAdId = firstActiveAdIdInCategory("cars");

        mockMvc.perform(get("/api/tuition/classes/" + carAdId))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveTuitionAdIsNotPubliclyReturned() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long pendingAdId = findMineAdIdByTitle(token, "A/L Physics - Individual Online Classes");

        mockMvc.perform(get("/api/tuition/classes/" + pendingAdId))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingOrInvalidSlugReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/tuition/classes/not-a-valid-slug"))
                .andExpect(status().isNotFound());
    }

    private long englishClassesForKidsAdId() throws Exception {
        String response = mockMvc.perform(get("/api/ads")
                        .param("category", "school-tuition")
                        .param("minPrice", "2200")
                        .param("maxPrice", "2200")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(response).get("content");
        assertEquals(1, content.size(), "expected exactly one seeded tuition ad priced at 2200");
        return content.get(0).get("id").asLong();
    }

    private long firstActiveAdIdInCategory(String categorySlug) throws Exception {
        String response = mockMvc.perform(get("/api/ads").param("category", categorySlug).param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(response).get("content");
        return content.get(0).get("id").asLong();
    }

    private long findMineAdIdByTitle(String token, String title) throws Exception {
        String response = mockMvc.perform(get("/api/ads/mine").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode ad : objectMapper.readTree(response)) {
            if (title.equals(ad.get("title").asText())) {
                return ad.get("id").asLong();
            }
        }
        fail("Seeded ad not found in kamal's own list: " + title);
        return -1;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
