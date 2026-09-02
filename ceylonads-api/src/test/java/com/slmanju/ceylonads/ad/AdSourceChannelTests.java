package com.slmanju.ceylonads.ad;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Covers source_channel: the main create/update flow only ever assigns/preserves MAIN_SITE, the
// MAIN public marketplace (search/detail/featured) only ever surfaces MAIN_SITE listings, a
// MODERATOR is scoped to MAIN_SITE while ADMIN stays cross-channel, and seller ownership (My Ads)
// is never channel-restricted.
//
// TUITION/BOARDING ads here are persisted directly through AdRepository (the same way
// LocalDataSeeder/TuitionPerformanceSeeder classify DEV data) since the public create API can
// only ever assign MAIN_SITE by design - there is no client-facing way to create a non-MAIN_SITE
// ad, which is itself part of what this suite verifies.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class AdSourceChannelTests {

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

    // --- Database / domain --------------------------------------------------------------------

    @Test
    void newAdDefaultsToMainSiteChannel() {
        Category category = categories.findBySlug("vehicles").orElseThrow();
        Customer seller = customers.findByAccountUsernameIgnoreCase("kamal").orElseThrow();
        Ad ad = ads.save(new Ad("Default Channel Ad " + UUID.randomUUID(),
                "A description long enough for validation.", new BigDecimal("1000"), category, seller));

        Ad reloaded = ads.findById(ad.getId()).orElseThrow();
        assertEquals(SourceChannel.MAIN_SITE, reloaded.getSourceChannel());
    }

    // --- MAIN create / update -------------------------------------------------------------------

    @Test
    void mainCreateAssignsMainSiteChannel() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token, "Main Create Channel " + UUID.randomUUID());

        assertEquals(SourceChannel.MAIN_SITE, ads.findById(adId).orElseThrow().getSourceChannel());
    }

    @Test
    void clientCannotSetSourceChannelThroughCreateRequest() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        String title = "Spoofed Channel Ad " + UUID.randomUUID();
        // sourceChannel isn't a field on CreateAdRequest, so this extra property is simply ignored
        // by Jackson - the ad is created as MAIN_SITE regardless of what the client sends.
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1000"),
                "categorySlug", "vehicles",
                "locationSlug", "colombo",
                "sourceChannel", "TUITION"));

        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long adId = objectMapper.readTree(response).get("id").asLong();

        assertEquals(SourceChannel.MAIN_SITE, ads.findById(adId).orElseThrow().getSourceChannel());
    }

    @Test
    void mainUpdateEndpointCannotReachAnOwnedNonMainSiteAd() throws Exception {
        // The MAIN update endpoint is scoped to MAIN_SITE, same as MAIN moderation is scoped to
        // MAIN_SITE: even the owning seller can't reach their own TUITION/BOARDING ad through it
        // (404, not a silent no-op) - "preserve source_channel" here means the endpoint can't
        // touch a foreign-channel ad at all, not just that it declines to change the field.
        // "vehicles" (rather than "school-tuition") avoids Tuition's required dynamic attributes -
        // this test is only about the channel boundary, not category rules.
        String originalTitle = "Tuition Update Preserve " + UUID.randomUUID();
        long adId = persistAd(SourceChannel.TUITION, "vehicles", originalTitle, AdStatus.ACTIVE);
        String token = loginAndGetToken("kamal", "customer123");

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "title", "Tuition Update Preserve Updated " + UUID.randomUUID(),
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1500"),
                "categorySlug", "vehicles",
                "locationSlug", "colombo"));
        mockMvc.perform(put("/api/ads/" + adId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isNotFound());

        Ad reloaded = ads.findById(adId).orElseThrow();
        assertEquals(SourceChannel.TUITION, reloaded.getSourceChannel());
        assertEquals(originalTitle, reloaded.getTitle());
    }

    @Test
    void updateCannotSwitchMainSiteAdToAnotherChannelThroughRequestPayload() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createAd(token, "Main Site Update Spoof " + UUID.randomUUID());
        assertEquals(SourceChannel.MAIN_SITE, ads.findById(adId).orElseThrow().getSourceChannel());

        // Same reasoning as clientCannotSetSourceChannelThroughCreateRequest: sourceChannel isn't a
        // field on CreateAdRequest, so a client can't flip an existing MAIN_SITE ad to TUITION by
        // sending it on an otherwise-normal update either - AdService.updateOwned() never reads or
        // assigns the channel at all.
        String updateBody = objectMapper.writeValueAsString(Map.of(
                "title", "Main Site Update Spoof Updated " + UUID.randomUUID(),
                "description", "A description long enough for validation.",
                "price", new BigDecimal("1500"),
                "categorySlug", "vehicles",
                "locationSlug", "colombo",
                "sourceChannel", "TUITION"));
        mockMvc.perform(put("/api/ads/" + adId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk());

        assertEquals(SourceChannel.MAIN_SITE, ads.findById(adId).orElseThrow().getSourceChannel());
    }

    // --- MAIN public search / count -------------------------------------------------------------

    @Test
    void defaultBrowseIncludesMainSiteAndExcludesTuitionAndBoarding() throws Exception {
        String marker = "ChannelBrowseMarker-" + UUID.randomUUID();
        String token = loginAndGetToken("kamal", "customer123");
        long mainSiteAdId = createApprovedAd(token, marker + " main site");
        persistAd(SourceChannel.TUITION, "school-tuition", marker + " tuition", AdStatus.ACTIVE);
        persistAd(SourceChannel.BOARDING, "vehicles", marker + " boarding", AdStatus.ACTIVE);

        // Content and count must agree: exactly the one MAIN_SITE ad, never the TUITION/BOARDING ones.
        mockMvc.perform(get("/api/ads").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mainSiteAdId));
    }

    @Test
    void categorySearchExcludesTuitionAdInTheSameCategory() throws Exception {
        String marker = "ChannelCategoryMarker-" + UUID.randomUUID();
        persistAd(SourceChannel.TUITION, "school-tuition", marker + " tuition", AdStatus.ACTIVE);

        mockMvc.perform(get("/api/ads").param("q", marker).param("category", "school-tuition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // --- MAIN public detail -----------------------------------------------------------------

    @Test
    void mainPublicDetailReturnsMainSiteAd() throws Exception {
        String token = loginAndGetToken("kamal", "customer123");
        long adId = createApprovedAd(token, "Main Detail Channel " + UUID.randomUUID());

        mockMvc.perform(get("/api/ads/" + adId)).andExpect(status().isOk());
    }

    @Test
    void mainPublicDetailDoesNotExposeTuitionOrBoardingAd() throws Exception {
        long tuitionAdId = persistAd(SourceChannel.TUITION, "school-tuition", "Hidden Tuition Detail " + UUID.randomUUID(), AdStatus.ACTIVE);
        long boardingAdId = persistAd(SourceChannel.BOARDING, "vehicles", "Hidden Boarding Detail " + UUID.randomUUID(), AdStatus.ACTIVE);

        mockMvc.perform(get("/api/ads/" + tuitionAdId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/ads/" + boardingAdId)).andExpect(status().isNotFound());
    }

    // --- MODERATOR ------------------------------------------------------------------------------

    @Test
    void moderatorPendingQueueExcludesTuitionAndBoarding() throws Exception {
        String marker = "ChannelQueueMarker-" + UUID.randomUUID();
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");
        String kamalToken = loginAndGetToken("kamal", "customer123");
        long mainSitePendingId = createAd(kamalToken, marker + " main pending");
        long tuitionPendingId = persistAd(SourceChannel.TUITION, "school-tuition", marker + " tuition pending", AdStatus.PENDING_REVIEW);
        long boardingPendingId = persistAd(SourceChannel.BOARDING, "vehicles", marker + " boarding pending", AdStatus.PENDING_REVIEW);

        mockMvc.perform(get("/api/moderation/ads/pending").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + mainSitePendingId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + tuitionPendingId + ")]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == " + boardingPendingId + ")]").doesNotExist());
    }

    @Test
    void moderatorCannotApproveOrRejectTuitionAd() throws Exception {
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");
        long tuitionPendingId = persistAd(SourceChannel.TUITION, "school-tuition", "Moderator Blocked Tuition " + UUID.randomUUID(), AdStatus.PENDING_REVIEW);

        mockMvc.perform(patch("/api/moderation/ads/" + tuitionPendingId + "/approve").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/moderation/ads/" + tuitionPendingId + "/reject").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isNotFound());

        assertEquals(AdStatus.PENDING_REVIEW, ads.findById(tuitionPendingId).orElseThrow().getStatus());
    }

    @Test
    void moderatorCannotBypassChannelRestrictionBySupplyingAnotherChannel() throws Exception {
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");
        long tuitionPendingId = persistAd(SourceChannel.TUITION, "school-tuition", "Moderator Bypass Attempt " + UUID.randomUUID(), AdStatus.PENDING_REVIEW);

        // The moderation endpoints never read a client-supplied channel at all - restriction is
        // resolved solely from the caller's own role (see ModerationController.restrictToChannel).
        // A query string claiming a different channel, or a request body on approve/reject (which
        // take none), has nothing to latch onto and changes nothing.
        mockMvc.perform(get("/api/moderation/ads/pending?sourceChannel=TUITION").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + tuitionPendingId + ")]").doesNotExist());

        mockMvc.perform(patch("/api/moderation/ads/" + tuitionPendingId + "/approve?sourceChannel=TUITION")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType("application/json")
                        .content("{\"sourceChannel\":\"TUITION\"}"))
                .andExpect(status().isNotFound());

        assertEquals(AdStatus.PENDING_REVIEW, ads.findById(tuitionPendingId).orElseThrow().getStatus());
    }

    @Test
    void moderatorCannotDeactivateBoardingAd() throws Exception {
        String moderatorToken = loginAndGetToken("moderator1", "moderator123");
        long boardingActiveId = persistAd(SourceChannel.BOARDING, "vehicles", "Moderator Blocked Boarding " + UUID.randomUUID(), AdStatus.ACTIVE);

        mockMvc.perform(patch("/api/moderation/ads/" + boardingActiveId + "/deactivate").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isNotFound());

        assertEquals(AdStatus.ACTIVE, ads.findById(boardingActiveId).orElseThrow().getStatus());
    }

    // --- ADMIN (cross-channel) --------------------------------------------------------------

    @Test
    void adminCanApproveTuitionAdViaModerationEndpoint() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long tuitionPendingId = persistAd(SourceChannel.TUITION, "school-tuition", "Admin Cross Channel Moderation " + UUID.randomUUID(), AdStatus.PENDING_REVIEW);

        mockMvc.perform(patch("/api/moderation/ads/" + tuitionPendingId + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void adminCanApproveTuitionAdViaAdminEndpoint() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        long tuitionPendingId = persistAd(SourceChannel.TUITION, "school-tuition", "Admin Endpoint Cross Channel " + UUID.randomUUID(), AdStatus.PENDING_REVIEW);

        mockMvc.perform(patch("/api/admin/ads/" + tuitionPendingId + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // --- SELLER / My Ads -------------------------------------------------------------------------

    @Test
    void sellerMyAdsIncludesOwnedAdsAcrossChannels() throws Exception {
        String marker = "ChannelMyAdsMarker-" + UUID.randomUUID();
        String token = loginAndGetToken("kamal", "customer123");
        long mainSiteAdId = createAd(token, marker + " main site mine");
        long tuitionAdId = persistAd(SourceChannel.TUITION, "school-tuition", marker + " tuition mine", AdStatus.ACTIVE);

        mockMvc.perform(get("/api/ads/mine").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + mainSiteAdId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + tuitionAdId + ")]").exists());
    }

    // --- Promotions -----------------------------------------------------------------------------

    @Test
    void mainHomeFeaturedExcludesTuitionAdEvenWithActivePromotion() throws Exception {
        String kamalToken = loginAndGetToken("kamal", "customer123");
        String adminToken = loginAndGetToken("admin", "admin123");
        long tuitionAdId = persistAd(SourceChannel.TUITION, "school-tuition", "Home Featured Tuition " + UUID.randomUUID(), AdStatus.ACTIVE);

        long planId = planIdByCode(kamalToken, "HOME_FEATURED_7D");
        String createResponse = mockMvc.perform(post("/api/promotions")
                        .header("Authorization", "Bearer " + kamalToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("adId", tuitionAdId, "promotionPlanId", planId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long promotionId = objectMapper.readTree(createResponse).get("id").asLong();
        mockMvc.perform(patch("/api/admin/promotions/" + promotionId + "/activate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/ads/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + tuitionAdId + ")]").doesNotExist());
    }

    // --- helpers --------------------------------------------------------------------------------

    private long persistAd(SourceChannel channel, String categorySlug, String title, AdStatus status) {
        Category category = categories.findBySlug(categorySlug).orElseThrow();
        Customer seller = customers.findByAccountUsernameIgnoreCase("kamal").orElseThrow();
        Ad ad = new Ad(title, "A description long enough for validation.", new BigDecimal("1000"), category, seller);
        ad.assignSourceChannel(channel);
        if (status == AdStatus.ACTIVE) {
            ad.approve(null);
        }
        return ads.save(ad).getId();
    }

    private long createAd(String token, String title) throws Exception {
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

    private long createApprovedAd(String token, String title) throws Exception {
        long id = createAd(token, title);
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        return id;
    }

    private long planIdByCode(String token, String code) throws Exception {
        String response = mockMvc.perform(get("/api/promotion-plans").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(response)) {
            if (node.get("code").asText().equals(code)) {
                return node.get("id").asLong();
            }
        }
        throw new IllegalStateException("Seed plan not found: " + code);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
