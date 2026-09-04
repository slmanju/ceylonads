package com.slmanju.ceylonads.tuition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.common.config.LocalDataSeeder;
import com.slmanju.ceylonads.common.util.Slugs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Covers GET /tuition/sitemap.xml (see TuitionSitemapService). Uses its own freshly created
// "Chess"-subject ads (never used by LocalDataSeeder's own tuition catalog - see
// LocalDataSeeder#tuitionAdSeeds) so subject/delivery/location combo assertions are exact and
// isolated from the shared seeded fixtures, which the sitemap will also legitimately include.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionSitemapTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalDataSeeder seeder;

    @Autowired
    private AdRepository ads;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void returnsWellFormedXmlWithHomepageAndSearchRoot() throws Exception {
        String xml = fetchSitemap();

        assertTrue(xml.contains("<loc>http://localhost:5174/</loc>"), "missing homepage: " + xml);
        assertTrue(xml.contains("<loc>http://localhost:5174/classes</loc>"), "missing /classes: " + xml);
        parseXmlOrFail(xml);
    }

    @Test
    void includesSubjectDeliveryAndLocationCombosWithActiveInventory() throws Exception {
        String token = registerAndGetToken();
        long onlineColombo = createClass(token, "Sitemap Chess Colombo", "Chess", "ONLINE", "colombo");
        long physicalKandy = createClass(token, "Sitemap Chess Kandy", "Chess", "PHYSICAL", "kandy");
        approveAsAdmin(onlineColombo);
        approveAsAdmin(physicalKandy);

        String xml = fetchSitemap();

        assertTrue(xml.contains("subject=Chess</loc>"), "missing subject-only page: " + xml);
        assertTrue(xml.contains("deliveryMode=ONLINE</loc>"), "missing deliveryMode-only page: " + xml);
        assertTrue(xml.contains("subject=Chess&amp;deliveryMode=ONLINE"), "missing subject+delivery combo: " + xml);
        assertTrue(xml.contains("subject=Chess&amp;deliveryMode=PHYSICAL"), "missing subject+delivery combo: " + xml);
        assertTrue(xml.contains("subject=Chess&amp;location=colombo"), "missing subject+location combo: " + xml);
        assertTrue(xml.contains("subject=Chess&amp;location=kandy"), "missing subject+location combo: " + xml);
        assertTrue(xml.contains(Slugs.adSlug("Sitemap Chess Colombo", onlineColombo)), "missing active class detail: " + xml);
        assertTrue(xml.contains(Slugs.adSlug("Sitemap Chess Kandy", physicalKandy)), "missing active class detail: " + xml);
    }

    @Test
    void everyLocStartsWithTheConfiguredTuitionSiteUrl() throws Exception {
        String token = registerAndGetToken();
        long id = createClass(token, "Sitemap Loc Prefix Check", "Chess", "ONLINE", "colombo");
        approveAsAdmin(id);

        String xml = fetchSitemap();
        List<String> locs = extractLocs(xml);

        assertTrue(locs.size() > 2, "expected more than just homepage/classes: " + xml);
        for (String loc : locs) {
            assertTrue(loc.startsWith("http://localhost:5174"), "unexpected <loc> not under the configured tuition-site-url: " + loc);
        }
    }

    @Test
    void mainSiteAdsAreExcludedFromTheTuitionSitemap() throws Exception {
        String token = registerAndGetToken();
        long mainSiteAdId = createMainSiteAd(token, "Sitemap Main Site Only Car");
        approveAsAdmin(mainSiteAdId);

        String xml = fetchSitemap();

        assertFalse(xml.contains(Slugs.adSlug("Sitemap Main Site Only Car", mainSiteAdId)),
                "a MAIN_SITE ad must never appear in the Tuition sitemap: " + xml);
    }

    @Test
    void excludesExpiredAndPendingListings() throws Exception {
        String token = registerAndGetToken();
        long expired = createClass(token, "Sitemap Chess Expired", "Chess", "PHYSICAL", "galle");
        long pending = createClass(token, "Sitemap Chess Pending", "Chess", "PHYSICAL", "jaffna");
        approveAsAdmin(expired);
        forceExpiry(expired, -1, AdStatus.ACTIVE);
        // pending is left un-approved, still PENDING_REVIEW.

        String xml = fetchSitemap();

        assertFalse(xml.contains(Slugs.adSlug("Sitemap Chess Expired", expired)), "expired listing must not be indexed: " + xml);
        assertFalse(xml.contains(Slugs.adSlug("Sitemap Chess Pending", pending)), "pending listing must not be indexed: " + xml);
    }

    @Test
    void doesNotIndexLowValueFilterCombinations() throws Exception {
        String xml = fetchSitemap();

        assertFalse(xml.contains("level="), "level-only/combined filters must not be sitemap-indexable: " + xml);
        assertFalse(xml.contains("sort="), "sort must not be sitemap-indexable: " + xml);
        assertFalse(xml.contains("page="), "page must not be sitemap-indexable: " + xml);
    }

    private String fetchSitemap() throws Exception {
        return mockMvc.perform(get("/tuition/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_XML))
                .andReturn().getResponse().getContentAsString();
    }

    private void parseXmlOrFail(String xml) throws Exception {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private List<String> extractLocs(String xml) throws Exception {
        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        var nodes = doc.getElementsByTagName("loc");
        List<String> locs = new java.util.ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            locs.add(nodes.item(i).getTextContent());
        }
        return locs;
    }

    // Generic (non-Tuition) ad creation - deliberately the plain /api/ads endpoint, which defaults
    // sourceChannel to MAIN_SITE (see AdService.create), to prove the Tuition sitemap never leaks
    // a main-storefront listing regardless of category/status.
    private long createMainSiteAd(String token, String title) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "A description long enough for validation purposes.");
        body.put("price", 1000);
        body.put("categorySlug", "vehicles");
        body.put("locationSlug", "colombo");
        String response = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createClass(String token, String title, String subject, String deliveryMode, String locationSlug) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "A description long enough for validation purposes.");
        body.put("price", 3000);
        body.put("categorySlug", "school-tuition");
        body.put("locationSlugs", List.of(locationSlug));
        body.put("subject", subject);
        body.put("curriculum", "LOCAL");
        body.put("medium", List.of("ENGLISH"));
        body.put("deliveryMode", deliveryMode);
        body.put("classFormat", "INDIVIDUAL");
        String response = mockMvc.perform(post("/api/tuition/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void approveAsAdmin(long id) throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(patch("/api/admin/ads/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private void forceExpiry(long id, int daysFromNow, AdStatus status) {
        Ad ad = ads.findById(id).orElseThrow();
        ad.seedExpiryOverride(Instant.now().plus(Duration.ofDays(daysFromNow)), status);
        ads.save(ad);
    }

    private String registerAndGetToken() throws Exception {
        String username = "tuition_sitemap_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        Map<String, Object> body = Map.of(
                "username", username,
                "password", "customer123",
                "email", username + "@example.test",
                "displayName", "Sitemap Test Tutor");
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
