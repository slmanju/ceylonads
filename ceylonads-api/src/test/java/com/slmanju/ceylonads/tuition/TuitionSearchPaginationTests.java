package com.slmanju.ceylonads.tuition;

import com.slmanju.ceylonads.ad.entity.Ad;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Covers GET /api/tuition/classes/search's 3x3 (size=9) default page size: the UI's expected
// [1][2][3] / [4][5][6] / [7][8][9] grid, correct totalPages/short-final-page arithmetic, and that
// the count/content stay TUITION-only (MAIN_SITE listings in the same category never leak in).
@SpringBootTest
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
class TuitionSearchPaginationTests {

    private static final String CATEGORY_SLUG = "school-tuition";

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

    @BeforeEach
    void seedData() throws Exception {
        seeder.run();
    }

    @Test
    void searchDefaultsToNineResultsPerPageAndSplitsTheFinalPageCorrectly() throws Exception {
        String marker = "PaginationMarker-" + UUID.randomUUID();
        for (int i = 0; i < 17; i++) {
            persistAd(SourceChannel.TUITION, marker + " class " + i);
        }

        mockMvc.perform(get("/api/tuition/classes/search").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(9))
                .andExpect(jsonPath("$.size").value(9))
                .andExpect(jsonPath("$.totalElements").value(17))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/tuition/classes/search").param("q", marker).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(8))
                .andExpect(jsonPath("$.totalElements").value(17))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void searchExcludesMainSiteListingsInTheSameCategory() throws Exception {
        String marker = "ChannelPaginationMarker-" + UUID.randomUUID();
        long tuitionAdId = persistAd(SourceChannel.TUITION, marker + " tuition");
        persistAd(SourceChannel.MAIN_SITE, marker + " main site");

        mockMvc.perform(get("/api/tuition/classes/search").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(tuitionAdId));
    }

    private long persistAd(SourceChannel channel, String title) {
        Category category = categories.findBySlug(CATEGORY_SLUG).orElseThrow();
        Customer seller = customers.findByAccountUsernameIgnoreCase("kamal").orElseThrow();
        Ad ad = new Ad(title, "A description long enough for validation.", new BigDecimal("1000"), category, seller);
        ad.assignSourceChannel(channel);
        ad.approve(null);
        return ads.save(ad).getId();
    }
}
