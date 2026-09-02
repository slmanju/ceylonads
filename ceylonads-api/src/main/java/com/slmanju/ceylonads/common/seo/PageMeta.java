package com.slmanju.ceylonads.common.seo;

// pageTitle is what <title> renders (e.g. "{ad title} | CeylonAds"); ogTitle/twitter:title use the
// bare title instead, per the site's existing convention (see Seo.tsx / AdDetailsPage) of not
// duplicating the "| CeylonAds" suffix in share-card titles.
public record PageMeta(
        String pageTitle,
        String ogTitle,
        String description,
        String imageUrl,
        String canonicalUrl,
        boolean noindex) {
}
