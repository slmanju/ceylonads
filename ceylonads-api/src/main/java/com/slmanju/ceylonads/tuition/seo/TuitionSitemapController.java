package com.slmanju.ceylonads.tuition.seo;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Served at /tuition/sitemap.xml, not /sitemap.xml - CeylonAds' own SitemapController already owns
// that path for the main-site domain. ezclass.lk is a separate static-hosted origin from this API
// (see ceylonads-tuition-ui), so making https://ezclass.lk/sitemap.xml resolve here requires one
// edge route (Cloudflare) forwarding that path to this endpoint.
@RestController
@Hidden
public class TuitionSitemapController {

    private final TuitionSitemapService sitemapService;

    public TuitionSitemapController(TuitionSitemapService sitemapService) {
        this.sitemapService = sitemapService;
    }

    @GetMapping(value = "/tuition/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    String sitemap() {
        return sitemapService.buildXml();
    }
}
