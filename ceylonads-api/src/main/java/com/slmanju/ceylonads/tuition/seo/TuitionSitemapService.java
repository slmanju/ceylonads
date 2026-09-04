package com.slmanju.ceylonads.tuition.seo;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdAttributeValue;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.repository.AdLocationRepository;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.ad.specification.AdSpecifications;
import com.slmanju.ceylonads.common.util.Slugs;
import com.slmanju.ceylonads.tuition.repository.TuitionAdAttributeValueRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

// Builds ezClass's own sitemap.xml. Distinct from CeylonAds' SitemapService (which covers only
// MAIN_SITE ads and links /ads/* on a different origin) since ezclass.lk is a separate frontend
// with its own /classes search architecture. SEO-worthy /classes?... URLs are derived from real
// active inventory only (never every possible subject/location combination), so the sitemap never
// links a page with zero results - see the task's "selective location indexing" requirement.
@Service
public class TuitionSitemapService {

    private static final String SUBJECT_KEY = "subject";
    private static final String DELIVERY_MODE_KEY = "classMode";

    // Any active inventory at all counts as "meaningful" for now - ezClass is a new site with a
    // small catalog, so a stricter threshold (e.g. >=2) would leave most of the sitemap empty.
    // Raise this once subject+location inventory is deep enough that thin pages become a real risk.
    private static final long MIN_SUBJECT_LOCATION_ADS = 1;

    private final AdRepository ads;
    private final TuitionAdAttributeValueRepository attributeValues;
    private final AdLocationRepository adLocations;
    private final String siteUrl;

    public TuitionSitemapService(
            AdRepository ads,
            TuitionAdAttributeValueRepository attributeValues,
            AdLocationRepository adLocations,
            @Value("${app.tuition-site-url}") String siteUrl) {
        this.ads = ads;
        this.attributeValues = attributeValues;
        this.adLocations = adLocations;
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
    }

    @Transactional(readOnly = true)
    public String buildXml() {
        Specification<Ad> activeTuition = Specification
                .<Ad>where((root, query, cb) -> cb.equal(root.get("status"), AdStatus.ACTIVE))
                .and((root, query, cb) -> cb.equal(root.get("sourceChannel"), SourceChannel.TUITION))
                .and(AdSpecifications.notExpired(Instant.now()));
        List<Ad> activeAds = ads.findAll(activeTuition);
        List<Long> adIds = activeAds.stream().map(Ad::getId).toList();

        Map<Long, String> subjectByAd = firstValueByAd(adIds, SUBJECT_KEY);
        Map<Long, String> deliveryModeByAd = firstValueByAd(adIds, DELIVERY_MODE_KEY);
        Map<Long, List<String>> locationSlugsByAd = adLocations.findByAdIdInOrderByAdIdAsc(adIds).stream()
                .collect(Collectors.groupingBy(
                        al -> al.getAd().getId(),
                        Collectors.mapping(al -> al.getLocation().getSlug(), Collectors.toList())));

        Map<String, Long> subjectCounts = countBy(activeAds, ad -> singleOrEmpty(subjectByAd.get(ad.getId())));
        Map<String, Long> deliveryModeCounts = countBy(activeAds, ad -> singleOrEmpty(deliveryModeByAd.get(ad.getId())));
        Map<String, Long> subjectDeliveryCounts = countBy(activeAds, ad ->
                combined(subjectByAd.get(ad.getId()), deliveryModeByAd.get(ad.getId())));
        Map<String, Long> subjectLocationCounts = countBy(activeAds, ad -> {
            String subject = subjectByAd.get(ad.getId());
            if (subject == null || subject.isBlank()) {
                return List.of();
            }
            return locationSlugsByAd.getOrDefault(ad.getId(), List.of()).stream()
                    .map(slug -> subject + "|" + slug)
                    .toList();
        });

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        appendUrl(xml, "/", "daily", "1.0", null);
        appendUrl(xml, "/classes", "daily", "0.9", null);

        subjectCounts.keySet().forEach(subject ->
                appendUrl(xml, "/classes?subject=" + encode(subject), "weekly", "0.7", null));

        deliveryModeCounts.keySet().forEach(mode ->
                appendUrl(xml, "/classes?deliveryMode=" + encode(mode), "weekly", "0.6", null));

        subjectDeliveryCounts.keySet().forEach(key -> {
            String[] parts = key.split("\\|", 2);
            appendUrl(xml, "/classes?subject=" + encode(parts[0]) + "&amp;deliveryMode=" + encode(parts[1]),
                    "weekly", "0.6", null);
        });

        subjectLocationCounts.entrySet().stream()
                .filter(e -> e.getValue() >= MIN_SUBJECT_LOCATION_ADS)
                .map(Map.Entry::getKey)
                .forEach(key -> {
                    String[] parts = key.split("\\|", 2);
                    appendUrl(xml, "/classes?subject=" + encode(parts[0]) + "&amp;location=" + encode(parts[1]),
                            "weekly", "0.6", null);
                });

        for (Ad ad : activeAds) {
            String path = "/classes/" + Slugs.adSlug(ad.getTitle(), ad.getId());
            String lastmod = DateTimeFormatter.ISO_INSTANT.format(ad.getUpdatedAt());
            appendUrl(xml, path, "weekly", "0.8", lastmod);
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    private Map<Long, String> firstValueByAd(List<Long> adIds, String key) {
        if (adIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new TreeMap<>();
        for (AdAttributeValue row : attributeValues.findByAdIdInAndKeyIn(adIds, List.of(key))) {
            result.putIfAbsent(row.getAd().getId(), row.getValueText());
        }
        return result;
    }

    private Map<String, Long> countBy(List<Ad> ads, Function<Ad, List<String>> keysFn) {
        Map<String, Long> counts = new TreeMap<>();
        for (Ad ad : ads) {
            for (String key : keysFn.apply(ad)) {
                counts.merge(key, 1L, Long::sum);
            }
        }
        return counts;
    }

    private List<String> singleOrEmpty(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value);
    }

    private List<String> combined(String subject, String deliveryMode) {
        if (subject == null || subject.isBlank() || deliveryMode == null || deliveryMode.isBlank()) {
            return List.of();
        }
        return List.of(subject + "|" + deliveryMode);
    }

    private String encode(String value) {
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
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
