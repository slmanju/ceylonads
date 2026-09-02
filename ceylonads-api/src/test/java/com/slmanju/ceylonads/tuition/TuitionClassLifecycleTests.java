package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers the dedicated Tuition lifecycle API (POST/PUT/DELETE/GET /api/tuition/classes,
// GET /api/tuition/my-classes): channel ownership (backend-assigned TUITION, never client-chosen),
// category-tree validation, attribute persistence via the existing shared AdAttributeService,
// reuse of AdService.createAd/updateAd/deactivateOwned (no duplicated Ad lifecycle logic), and that
// the existing MAIN /api/ads/** flow and moderation boundary are unaffected.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionClassLifecycleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private AdRepository ads;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private CustomerRepository customers;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    // --- CREATE ---------------------------------------------------------------------------------

    @Test
    void tuitionCreateAssignsTuitionChannelAutomatically() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Combined Maths A/L " + UUID.randomUUID()));

        assertEquals(SourceChannel.TUITION, ads.findById(id).orElseThrow().getSourceChannel());
    }

    @Test
    void requestCannotChooseMainSiteOrBoardingChannel() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        Map<String, Object> body = tuitionBody("Spoofed Channel Class " + UUID.randomUUID());
        body.put("sourceChannel", "MAIN_SITE");

        String response = mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        assertEquals(SourceChannel.TUITION, ads.findById(id).orElseThrow().getSourceChannel());
    }

    @Test
    void validEducationTuitionCategorySucceeds() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(tuitionBody("Category Root " + UUID.randomUUID()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categorySlug").value("school-tuition"))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    void unrelatedCategoryIsRejected() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        Map<String, Object> body = tuitionBody("Wrong Category Class " + UUID.randomUUID());
        body.put("categorySlug", "vehicles");

        mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tuitionAttributesArePersistedCorrectly() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Attribute Persistence Class " + UUID.randomUUID()));
        approveAsAdmin(id);

        mockMvc.perform(get("/api/tuition/classes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academic.subject").value("Combined Mathematics"))
                .andExpect(jsonPath("$.academic.level").value("A/L"))
                .andExpect(jsonPath("$.academic.curriculum.value").value("LOCAL"))
                .andExpect(jsonPath("$.academic.curriculum.label").value("Local"))
                .andExpect(jsonPath("$.academic.medium[0].value").value("ENGLISH"))
                .andExpect(jsonPath("$.classInfo.deliveryModes[0].value").value("PHYSICAL"))
                .andExpect(jsonPath("$.classInfo.classFormats[0].value").value("INDIVIDUAL"));
    }

    @Test
    void optionalLevelWorksWhenOmitted() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        Map<String, Object> body = tuitionBody("No Level Class " + UUID.randomUUID());
        body.remove("level");

        long id = createTuitionClass(token, body);
        approveAsAdmin(id);

        mockMvc.perform(get("/api/tuition/classes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academic.level").doesNotExist());
    }

    @Test
    void commonStatusModerationLifecyclePreserved() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Lifecycle Class " + UUID.randomUUID()));

        // Not public yet - matches the generic ad lifecycle (create -> pending -> moderation).
        mockMvc.perform(get("/api/tuition/classes/" + id)).andExpect(status().isNotFound());

        approveAsAdmin(id);

        mockMvc.perform(get("/api/tuition/classes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // --- UPDATE ---------------------------------------------------------------------------------

    @Test
    void ownerCanUpdateTuitionListing() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Update Owner Class " + UUID.randomUUID()));

        Map<String, Object> update = tuitionBody("Updated Title " + UUID.randomUUID());
        mockMvc.perform(put("/api/tuition/classes/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(update.get("title")));
    }

    @Test
    void updatePreservesTuitionChannel() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Update Preserve Class " + UUID.randomUUID()));

        mockMvc.perform(put("/api/tuition/classes/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(tuitionBody("Updated " + UUID.randomUUID()))))
                .andExpect(status().isOk());

        assertEquals(SourceChannel.TUITION, ads.findById(id).orElseThrow().getSourceChannel());
    }

    @Test
    void updateCannotSwitchChannelThroughRequestPayload() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Update Spoof Class " + UUID.randomUUID()));

        Map<String, Object> update = tuitionBody("Update Spoof Updated " + UUID.randomUUID());
        update.put("sourceChannel", "BOARDING");
        mockMvc.perform(put("/api/tuition/classes/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        assertEquals(SourceChannel.TUITION, ads.findById(id).orElseThrow().getSourceChannel());
    }

    @Test
    void tuitionEndpointCannotUpdateMainSiteListing() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long mainSiteId = createMainSiteAd(token, "Main Site For Tuition Update " + UUID.randomUUID());

        mockMvc.perform(put("/api/tuition/classes/" + mainSiteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(tuitionBody("Hijacked " + UUID.randomUUID()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void tuitionEndpointCannotUpdateBoardingListing() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long boardingId = persistChannelAd(SourceChannel.BOARDING, "vehicles", "Boarding For Tuition Update " + UUID.randomUUID());

        mockMvc.perform(put("/api/tuition/classes/" + boardingId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(tuitionBody("Hijacked " + UUID.randomUUID()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unrelatedCategoryChangeRejectedOnUpdate() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Category Change Class " + UUID.randomUUID()));

        Map<String, Object> update = tuitionBody("Category Change Updated " + UUID.randomUUID());
        update.put("categorySlug", "vehicles");
        mockMvc.perform(put("/api/tuition/classes/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attributesUpdateCorrectly() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Attribute Update Class " + UUID.randomUUID()));
        approveAsAdmin(id);

        Map<String, Object> update = tuitionBody("Attribute Update Class Updated " + UUID.randomUUID());
        update.put("subject", "Physics");
        update.put("curriculum", "CAMBRIDGE");
        mockMvc.perform(put("/api/tuition/classes/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academic.subject").value("Physics"))
                .andExpect(jsonPath("$.academic.curriculum.value").value("CAMBRIDGE"));
    }

    // --- DELETE ---------------------------------------------------------------------------------

    @Test
    void ownerCanDeactivateOwnTuitionListing() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Delete Owner Class " + UUID.randomUUID()));
        approveAsAdmin(id);

        mockMvc.perform(delete("/api/tuition/classes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Same semantics as the generic ad delete: a status change, not a hard row delete.
        Ad reloaded = ads.findById(id).orElseThrow();
        assertEquals(AdStatus.DEACTIVATED, reloaded.getStatus());
    }

    @Test
    void tuitionEndpointCannotDeleteMainSiteListing() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long mainSiteId = createMainSiteAd(token, "Main Site For Tuition Delete " + UUID.randomUUID());

        mockMvc.perform(delete("/api/tuition/classes/" + mainSiteId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        assertEquals(AdStatus.PENDING_REVIEW, ads.findById(mainSiteId).orElseThrow().getStatus());
    }

    @Test
    void sellerCannotDeleteAnotherSellersListing() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(kamalToken, tuitionBody("Not Yours Class " + UUID.randomUUID()));

        String nimalToken = loginAndGetToken("nimal", "customer123");
        mockMvc.perform(delete("/api/tuition/classes/" + id).header("Authorization", "Bearer " + nimalToken))
                .andExpect(status().isForbidden());
    }

    // --- READ -------------------------------------------------------------------------------

    @Test
    void tuitionDetailReturnsTuitionListing() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Detail Class " + UUID.randomUUID()));
        approveAsAdmin(id);

        mockMvc.perform(get("/api/tuition/classes/" + id)).andExpect(status().isOk());
    }

    @Test
    void tuitionDetailExcludesMainSiteListing() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long mainSiteId = createMainSiteAd(token, "Main Site For Tuition Detail " + UUID.randomUUID());
        approveAsAdmin(mainSiteId);

        mockMvc.perform(get("/api/tuition/classes/" + mainSiteId)).andExpect(status().isNotFound());
    }

    @Test
    void unpublishedListingFollowsExistingPublicVisibilityRules() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("Unpublished Class " + UUID.randomUUID()));

        mockMvc.perform(get("/api/tuition/classes/" + id)).andExpect(status().isNotFound());
    }

    // --- MY CLASSES -----------------------------------------------------------------------------

    @Test
    void myClassesReturnsSellersTuitionListings() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody("My Classes Marker " + UUID.randomUUID()));

        mockMvc.perform(get("/api/tuition/my-classes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
    }

    @Test
    void myClassesExcludesSellersMainSiteListings() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long mainSiteId = createMainSiteAd(token, "Main Site Not In My Classes " + UUID.randomUUID());

        mockMvc.perform(get("/api/tuition/my-classes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + mainSiteId + ")]").doesNotExist());
    }

    @Test
    void myClassesExcludesOtherSellersListings() throws Exception {
        String nimalToken = loginAndGetToken("nimal", "customer123");
        long nimalClassId = createTuitionClass(nimalToken, tuitionBody("Nimal Class " + UUID.randomUUID()));

        String kamalToken = loginAndGetToken("kamal", "customer123");
        mockMvc.perform(get("/api/tuition/my-classes").header("Authorization", "Bearer " + kamalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + nimalClassId + ")]").doesNotExist());
    }

    // --- MODERATION / SECURITY --------------------------------------------------------------

    @Test
    void mainModeratorCannotModerateTuitionListingCreatedThroughTheNewApi() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(kamalToken, tuitionBody("Moderator Blocked Class " + UUID.randomUUID()));

        String moderatorToken = loginAndGetToken("moderator1", "moderator123");
        mockMvc.perform(patch("/api/moderation/ads/" + id + "/approve").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanModerateTuitionListingCreatedThroughTheNewApi() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(kamalToken, tuitionBody("Admin Moderation Class " + UUID.randomUUID()));

        approveAsAdmin(id);
        assertEquals(AdStatus.ACTIVE, ads.findById(id).orElseThrow().getStatus());
    }

    // --- REGRESSION -------------------------------------------------------------------------

    @Test
    void existingMainCreateAndUpdateStillWork() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String body = """
                {"title":"Regression Main Ad %s","description":"A description long enough for validation.",\
                "price":1000,"categorySlug":"vehicles","locationSlug":"colombo"}
                """.formatted(UUID.randomUUID());

        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        String updateBody = """
                {"title":"Regression Main Ad Updated %s","description":"A description long enough for validation.",\
                "price":1500,"categorySlug":"vehicles","locationSlug":"colombo"}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(put("/api/ads/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk());

        assertEquals(SourceChannel.MAIN_SITE, ads.findById(id).orElseThrow().getSourceChannel());
    }

    @Test
    void existingGenericSearchExcludesTuitionListingsCreatedThroughTheNewApi() throws Exception {
        String marker = "TuitionApiSearchMarker-" + UUID.randomUUID();
        String token = loginAndGetToken("kamal", "customer123");
        long id = createTuitionClass(token, tuitionBody(marker));
        approveAsAdmin(id);

        mockMvc.perform(get("/api/ads").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // --- helpers --------------------------------------------------------------------------------

    private Map<String, Object> tuitionBody(String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "A description long enough for validation purposes.");
        body.put("price", 3000);
        body.put("categorySlug", "school-tuition");
        body.put("locationSlugs", List.of("colombo"));
        body.put("subject", "Combined Mathematics");
        body.put("level", "A/L");
        body.put("curriculum", "LOCAL");
        body.put("medium", List.of("ENGLISH"));
        body.put("deliveryMode", "PHYSICAL");
        body.put("classFormat", "INDIVIDUAL");
        return body;
    }

    private long createTuitionClass(String token, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createMainSiteAd(String token, String title) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1000"),
                "categorySlug", "vehicles",
                "locationSlug", "colombo"));
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    // Direct repository insert, mirroring how DEV/test fixtures create non-MAIN_SITE ads today -
    // there is still no client-facing way to create a BOARDING ad (out of scope for this task).
    private long persistChannelAd(SourceChannel channel, String categorySlug, String title) {
        Category category = categories.findBySlug(categorySlug).orElseThrow();
        Customer seller = customers.findByAccountUsernameIgnoreCase("kamal").orElseThrow();
        Ad ad = new Ad(title, "A description long enough for validation.", new BigDecimal("1000"), category, seller);
        ad.assignSourceChannel(channel);
        return ads.save(ad).getId();
    }

    private void approveAsAdmin(long id) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
