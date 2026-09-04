package com.slmanju.ceylonads.tuition.seo;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.repository.AdLocationRepository;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.tuition.repository.TuitionAdAttributeValueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Plain unit test (no Spring context, no DB) covering the one thing that's easy to get wrong
// purely from configuration: which base URL ends up in <loc>. A full-context test would only ever
// exercise whatever app.tuition-site-url happens to resolve to for the active test profile - this
// isolates the property-driven behavior itself, including the exact bug that shipped to production
// once already (a configured value with no scheme, or the wrong property, producing bad <loc>s).
@ExtendWith(MockitoExtension.class)
class TuitionSitemapServiceTests {

    @Mock
    private AdRepository ads;

    @Mock
    private TuitionAdAttributeValueRepository attributeValues;

    @Mock
    private AdLocationRepository adLocations;

    @Test
    void usesTheConfiguredProductionUrlWithNoLocalhostAnywhere() {
        when(ads.findAll(ArgumentMatchers.<Specification<Ad>>any())).thenReturn(List.of());
        when(adLocations.findByAdIdInOrderByAdIdAsc(any())).thenReturn(List.of());

        TuitionSitemapService service = new TuitionSitemapService(ads, attributeValues, adLocations, "https://ezclass.lk");
        String xml = service.buildXml();

        assertTrue(xml.contains("<loc>https://ezclass.lk/</loc>"), "expected homepage under the configured URL: " + xml);
        assertTrue(xml.contains("<loc>https://ezclass.lk/classes</loc>"), "expected /classes under the configured URL: " + xml);
        assertFalse(xml.contains("localhost"), "no localhost URL should appear once a production URL is configured: " + xml);
    }

    @Test
    void normalizesATrailingSlashOnTheConfiguredUrl() {
        when(ads.findAll(ArgumentMatchers.<Specification<Ad>>any())).thenReturn(List.of());
        when(adLocations.findByAdIdInOrderByAdIdAsc(any())).thenReturn(List.of());

        TuitionSitemapService service = new TuitionSitemapService(ads, attributeValues, adLocations, "https://ezclass.lk/");
        String xml = service.buildXml();

        assertTrue(xml.contains("<loc>https://ezclass.lk/</loc>"), "trailing slash on the configured URL must not produce a double slash: " + xml);
        assertFalse(xml.contains("ezclass.lk//"), "double slash indicates the trailing slash wasn't normalized: " + xml);
    }
}
