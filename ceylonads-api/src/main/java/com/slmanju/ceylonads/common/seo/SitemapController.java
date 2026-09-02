package com.slmanju.ceylonads.common.seo;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Served at the site root (not under /api) because the sitemap protocol only allows a sitemap
// to list URLs at or below the path it's hosted on - the pages it lists (/, /category/*, /ads/*)
// are not under /api, so the sitemap can't live there either.
@RestController
@Hidden
public class SitemapController {

    private final SitemapService sitemapService;

    public SitemapController(SitemapService sitemapService) {
        this.sitemapService = sitemapService;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    String sitemap() {
        return sitemapService.buildXml();
    }
}
