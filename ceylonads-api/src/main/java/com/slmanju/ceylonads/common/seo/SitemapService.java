package com.slmanju.ceylonads.common.seo;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.category.dto.CategoryResponse;
import com.slmanju.ceylonads.category.service.CategoryService;
import com.slmanju.ceylonads.common.util.Slugs;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.location.service.LocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

// Builds sitemap.xml from live data instead of a static file: category/location pages are few and
// stable, but ad pages come and go with every post/approval/deactivation, so a static sitemap
// would drift out of date almost immediately.
@Service
public class SitemapService {

    private final AdRepository ads;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final String siteUrl;

    public SitemapService(
            AdRepository ads,
            CategoryService categoryService,
            LocationService locationService,
            @Value("${app.public-site-url}") String siteUrl) {
        this.ads = ads;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
    }

    @Transactional(readOnly = true)
    public String buildXml() {
        List<CategoryResponse> categories = categoryService.findAllActive();
        List<LocationResponse> locations = locationService.findAllActive();
        // /ads/{slug} is now a MAIN-storefront-only public route (see
        // AdRepository.findDetailByIdAndStatusAndSourceChannel), so only MAIN_SITE ads belong here.
        List<Ad> activeAds = ads.findTop2000ByStatusAndSourceChannelOrderByCreatedAtDesc(AdStatus.ACTIVE, SourceChannel.MAIN_SITE);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        appendUrl(xml, "", "daily", "1.0", null);

        for (CategoryResponse category : categories) {
            appendUrl(xml, "/category/" + category.slug(), "daily", "0.8", null);
        }

        // Only top-level categories get combined with locations - crossing every leaf category
        // with every location would multiply into a large set of pages with little distinct
        // content, which is exactly the "SEO explosion" the task asks to avoid.
        List<CategoryResponse> topLevelCategories = categories.stream()
                .filter(c -> c.parentId() == null)
                .toList();
        for (CategoryResponse category : topLevelCategories) {
            for (LocationResponse location : locations) {
                appendUrl(xml, "/" + category.slug() + "/" + location.slug(), "weekly", "0.6", null);
            }
        }

        for (Ad ad : activeAds) {
            String path = "/ads/" + Slugs.adSlug(ad.getTitle(), ad.getId());
            String lastmod = DateTimeFormatter.ISO_INSTANT.format(ad.getUpdatedAt());
            appendUrl(xml, path, "weekly", "0.7", lastmod);
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String path, String changefreq, String priority, String lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(siteUrl).append(path).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}
