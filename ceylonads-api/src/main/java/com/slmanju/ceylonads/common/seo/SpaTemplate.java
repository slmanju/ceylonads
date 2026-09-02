package com.slmanju.ceylonads.common.seo;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Wraps the built SPA shell (static/index.html) so a request handler can swap in page-specific
// metadata without touching React/Vite's own output. The shell is read from the classpath once
// (it never changes at runtime) and split around the "seo:start"/"seo:end" markers that
// frontend/index.html carries through the Vite build untouched; render() then reassembles
// prefix + fresh <title>/meta/canonical tags + suffix for each request.
@Component
public class SpaTemplate {

    private static final String START_MARKER = "<!--seo:start-->";
    private static final String END_MARKER = "<!--seo:end-->";

    private String prefix;
    private String suffix;

    @PostConstruct
    void load() throws IOException {
        String html = StreamUtils.copyToString(
                new ClassPathResource("static/index.html").getInputStream(), StandardCharsets.UTF_8);
        int start = html.indexOf(START_MARKER);
        int end = html.indexOf(END_MARKER);
        if (start < 0 || end < 0) {
            throw new IllegalStateException("static/index.html is missing seo:start/seo:end markers");
        }
        this.prefix = html.substring(0, start);
        this.suffix = html.substring(end + END_MARKER.length());
    }

    public String render(PageMeta meta) {
        StringBuilder tags = new StringBuilder();
        String title = HtmlUtils.htmlEscape(meta.pageTitle());
        String ogTitle = HtmlUtils.htmlEscape(meta.ogTitle());
        String description = HtmlUtils.htmlEscape(meta.description());
        String image = HtmlUtils.htmlEscape(meta.imageUrl());
        String url = HtmlUtils.htmlEscape(meta.canonicalUrl());
        String robots = meta.noindex() ? "noindex, follow" : "index, follow";

        tags.append("<title>").append(title).append("</title>\n");
        tags.append("<meta name=\"description\" content=\"").append(description).append("\" />\n");
        tags.append("<meta name=\"robots\" content=\"").append(robots).append("\" />\n");
        tags.append("<meta property=\"og:site_name\" content=\"CeylonAds\" />\n");
        tags.append("<meta property=\"og:type\" content=\"website\" />\n");
        tags.append("<meta property=\"og:title\" content=\"").append(ogTitle).append("\" />\n");
        tags.append("<meta property=\"og:description\" content=\"").append(description).append("\" />\n");
        tags.append("<meta property=\"og:image\" content=\"").append(image).append("\" />\n");
        tags.append("<meta property=\"og:url\" content=\"").append(url).append("\" />\n");
        tags.append("<meta name=\"twitter:card\" content=\"summary_large_image\" />\n");
        tags.append("<meta name=\"twitter:title\" content=\"").append(ogTitle).append("\" />\n");
        tags.append("<meta name=\"twitter:description\" content=\"").append(description).append("\" />\n");
        tags.append("<meta name=\"twitter:image\" content=\"").append(image).append("\" />\n");
        tags.append("<link rel=\"canonical\" href=\"").append(url).append("\" />");

        return prefix + tags + suffix;
    }
}
