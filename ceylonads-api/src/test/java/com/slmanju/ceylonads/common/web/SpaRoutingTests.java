package com.slmanju.ceylonads.common.web;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.auth.entity.Account;
import com.slmanju.ceylonads.auth.entity.Role;
import com.slmanju.ceylonads.auth.repository.AccountRepository;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.customer.repository.CustomerRepository;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.repository.MediaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Covers the public SPA routing/metadata behavior described in the CeylonAds SPA routing fix:
// browse routes stay browse routes, /ads/{slug} gets server-rendered ad metadata for a valid ad
// and a real HTML 404 for an invalid one, and none of this leaks into /api, swagger, or static
// assets.
//
// Builds its own ad directly through the repositories (rather than the login + create + admin
// approve flow other tests in this suite use) so it doesn't depend on LocalDataSeeder's seed
// accounts/categories/locations, which are currently disabled independently of this change.
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
@Transactional
class SpaRoutingTests {

    private static final String DEFAULT_TITLE_ESCAPED =
            HtmlUtils.htmlEscape("CeylonAds — Sri Lanka's Trusted Marketplace");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private CustomerRepository customers;
    @Autowired
    private CategoryRepository categories;
    @Autowired
    private AdRepository ads;
    @Autowired
    private MediaRepository media;

    @Test
    void browseAdsReturnsTheSpaShellAndIsNotTreatedAsASlugLookup() throws Exception {
        MvcResult result = mockMvc.perform(get("/ads"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertBootsReactShell(body);
        assertThat(body).contains("<title>" + DEFAULT_TITLE_ESCAPED + "</title>");
    }

    @Test
    void browseAdsWithQueryStringStillReturnsTheBrowseShell() throws Exception {
        mockMvc.perform(get("/ads").param("q", "teaching"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        mockMvc.perform(get("/ads").param("q", "tea").param("location", "colombo"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void rootLoginAndRegisterReturnTheSpaShellAnonymously() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        mockMvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        mockMvc.perform(get("/register")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void validAdSlugReturnsHtmlWithServerRenderedAdMetadata() throws Exception {
        String slug = persistActiveAdWithMedia();

        MvcResult result = mockMvc.perform(get("/ads/" + slug))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertBootsReactShell(body);
        String escapedTitle = HtmlUtils.htmlEscape("Dehydrated Jack Fruits | CeylonAds");
        assertThat(body).contains("<title>" + escapedTitle + "</title>");
        assertContainsTag(body, "meta name=\"description\"", "Sun-dried jack fruit chips");
        assertContainsTag(body, "meta property=\"og:title\"", "Dehydrated Jack Fruits");
        assertContainsTag(body, "meta property=\"og:description\"", "Sun-dried jack fruit chips");
        // Local media storage returns a storage-relative URL ("/media/..."); the page must make
        // it absolute using app.public-site-url rather than depending on a persisted full URL.
        assertContainsTag(body, "meta property=\"og:image\"", "http://localhost:5173/media/");
        assertContainsTag(body, "meta property=\"og:url\"", "http://localhost:5173/ads/" + slug);
        assertContainsTag(body, "meta name=\"twitter:title\"", "Dehydrated Jack Fruits");
        assertContainsTag(body, "meta name=\"twitter:description\"", "Sun-dried jack fruit chips");
        assertContainsTag(body, "meta name=\"twitter:image\"", "http://localhost:5173/media/");
        assertContainsTag(body, "link rel=\"canonical\"", "http://localhost:5173/ads/" + slug);
        assertContainsTag(body, "meta name=\"twitter:card\"", "summary_large_image");
    }

    @Test
    void invalidAdSlugReturnsRealHtml404WithGenericMetadataNotBackendJson() throws Exception {
        MvcResult result = mockMvc.perform(get("/ads/something-that-does-not-exist-999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertBootsReactShell(body);
        assertThat(body).contains("<title>" + DEFAULT_TITLE_ESCAPED + "</title>");
        assertThat(body).doesNotContain("\"status\"").doesNotContain("\"error\"");
    }

    @Test
    void protectedApiEndpointsRemainProtected() throws Exception {
        mockMvc.perform(get("/api/admin/ads/pending")).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownApiRoutesAreNotForwardedToTheSpa() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/this-does-not-exist")).andReturn();
        String contentType = result.getResponse().getContentType();
        assertThat(contentType)
                .as("unmatched /api/** paths must not fall back to the SPA shell")
                .doesNotContain(MediaType.TEXT_HTML_VALUE);
    }

    @Test
    void swaggerAndOpenApiStillWork() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    private String persistActiveAdWithMedia() {
        Account account = accounts.save(new Account("seo-test-seller", "hash", "seo-test-seller@example.com", Role.CUSTOMER));
        Customer seller = customers.save(new Customer(account, "SEO Test Seller", "0770000000"));
        Category category = categories.save(new Category("Groceries", "seo-test-groceries", null, 0));

        Ad ad = new Ad(
                "Dehydrated Jack Fruits",
                "Sun-dried jack fruit chips, no preservatives, packed fresh weekly for wholesale and retail buyers across Colombo.",
                new BigDecimal("1500"),
                category,
                seller);
        ad.approve(null);
        ad = ads.save(ad);

        media.save(new Media(ad, "seo-test-image.jpg", "image/jpeg", 0));

        return com.slmanju.ceylonads.common.util.Slugs.adSlug(ad.getTitle(), ad.getId());
    }

    private void assertBootsReactShell(String html) {
        assertThat(html).contains("<div id=\"root\"></div>").contains("<script type=\"module\"");
    }

    private void assertContainsTag(String html, String tagPrefix, String expectedFragment) {
        int index = html.indexOf(tagPrefix);
        assertThat(index).as(tagPrefix + " tag present").isNotNegative();
        int lineEnd = html.indexOf('\n', index);
        String line = html.substring(index, lineEnd < 0 ? html.length() : lineEnd);
        assertThat(line).contains(expectedFragment);
    }
}
