package com.slmanju.ceylonads.common.web;

import com.slmanju.ceylonads.common.seo.AdPageHtml;
import com.slmanju.ceylonads.common.seo.SeoMetaService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// Serves the built React SPA shell for public frontend page routes, with server-rendered
// metadata so direct loads/refreshes/social crawlers see a real page instead of the JSON 401/404
// that Spring Security's/API's default handling would otherwise produce for these paths. Deliberately
// narrow: only "/", "/ads", "/ads/{slug}", "/login" and "/register" are mapped here (matching what
// SecurityConfig permits) rather than a catch-all "/**", so this can never shadow /api, /assets,
// /media, swagger, or the sitemap.
@RestController
@Hidden
public class SpaController {

    private static final MediaType HTML_UTF8 = new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8);

    private final SeoMetaService seoMetaService;

    public SpaController(SeoMetaService seoMetaService) {
        this.seoMetaService = seoMetaService;
    }

    @GetMapping({"/", "/ads", "/login", "/register"})
    ResponseEntity<String> shell(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean noindex = !("/".equals(path) || "/ads".equals(path));
        String html = seoMetaService.renderGeneric(path, noindex);
        return ResponseEntity.ok().contentType(HTML_UTF8).body(html);
    }

    @GetMapping("/ads/{slug}")
    ResponseEntity<String> adDetail(@PathVariable String slug) {
        AdPageHtml page = seoMetaService.renderForAdSlug(slug);
        HttpStatus status = page.status();
        return ResponseEntity.status(status).contentType(HTML_UTF8).body(page.html());
    }
}
