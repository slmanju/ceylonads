package com.slmanju.ceylonads.promotion.entity;
//com.slmanju.ceylonads.promotion.entity.PlacementType.AD_DETAIL_SIDEBAR
public enum PlacementType {
    HOME_FEATURED,
    HOME_BANNER,
    CATEGORY_FEATURED,
    CATEGORY_BANNER,
    TOP_SEARCH,
    AD_DETAIL_SIDEBAR;

    public boolean isBanner() {
        return this == HOME_BANNER || this == CATEGORY_BANNER;
    }

    public boolean isCategoryScoped() {
        return this == CATEGORY_FEATURED || this == CATEGORY_BANNER;
    }
}
