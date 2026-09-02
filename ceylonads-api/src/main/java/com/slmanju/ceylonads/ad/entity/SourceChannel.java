package com.slmanju.ceylonads.ad.entity;

// Which storefront/vertical owns a listing (as opposed to Category, which says what the listing
// is about). MAIN_SITE is the default/main CeylonAds marketplace; TUITION and BOARDING are
// separate storefronts layered on the same ads table. See V12__ad_source_channel.sql.
public enum SourceChannel {
    MAIN_SITE,
    TUITION,
    BOARDING
}
