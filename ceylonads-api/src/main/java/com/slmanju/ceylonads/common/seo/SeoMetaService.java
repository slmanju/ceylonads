package com.slmanju.ceylonads.common.seo;

import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.ad.service.AdService;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SeoMetaService {

    private static final String DEFAULT_TITLE = "CeylonAds — Sri Lanka's Trusted Marketplace";
    private static final String DEFAULT_DESCRIPTION =
            "Sri Lanka's trusted marketplace to buy and sell vehicles, property, mobiles, tuition and services.";
    private static final String DEFAULT_IMAGE_PATH = "/og/og-default.png";
    private static final int DESCRIPTION_MAX_LENGTH = 160;

    private final SpaTemplate template;
    private final AdService adService;
    private final String baseUrl;

    public SeoMetaService(
            SpaTemplate template,
            AdService adService,
            @Value("${app.public-site-url}") String baseUrl) {
        this.template = template;
        this.adService = adService;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    // Generic shell for "/", "/ads" (with or without a query string), "/login", "/register", etc.
    // - always the site's own default metadata, never anything ad-specific.
    public String renderGeneric(String path, boolean noindex) {
        PageMeta meta = new PageMeta(
                DEFAULT_TITLE,
                DEFAULT_TITLE,
                DEFAULT_DESCRIPTION,
                absolute(DEFAULT_IMAGE_PATH),
                absolute(path),
                noindex);
        return template.render(meta);
    }

    // Reuses AdService's existing public slug/id resolution and ACTIVE-only visibility rule -
    // no duplicate lookup or visibility logic here. A slug that doesn't resolve to a public ad
    // (unknown, pending, deactivated, etc.) falls back to the generic shell with a 404 status,
    // exactly like an unmatched client route, rather than leaking ad-specific data for a page
    // that isn't actually public.
    public AdPageHtml renderForAdSlug(String slug) {
        AdResponse ad;
        try {
            ad = adService.getPublic(slug);
        } catch (NotFoundException e) {
            String html = renderGeneric("/ads/" + slug, true);
            return new AdPageHtml(html, HttpStatus.NOT_FOUND);
        }

        String canonicalUrl = absolute("/ads/" + ad.slug());
        String description = ad.description() == null || ad.description().isBlank()
                ? DEFAULT_DESCRIPTION
                : truncate(ad.description());
        String image = ad.media().isEmpty() ? absolute(DEFAULT_IMAGE_PATH) : absolute(ad.media().get(0).url());

        PageMeta meta = new PageMeta(
                ad.title() + " | CeylonAds",
                ad.title(),
                description,
                image,
                canonicalUrl,
                false);
        return new AdPageHtml(template.render(meta), HttpStatus.OK);
    }

    private String absolute(String pathOrUrl) {
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        return baseUrl + (pathOrUrl.startsWith("/") ? pathOrUrl : "/" + pathOrUrl);
    }

    private String truncate(String text) {
        String collapsed = text.trim().replaceAll("\\s+", " ");
        if (collapsed.length() <= DESCRIPTION_MAX_LENGTH) {
            return collapsed;
        }
        return collapsed.substring(0, DESCRIPTION_MAX_LENGTH - 1).stripTrailing() + "…";
    }
}
