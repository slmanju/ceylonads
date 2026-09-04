package com.slmanju.ceylonads.admin.dto;

// Which slice of the Tuition promotion plan catalog AdminTuitionPromotionPlanController.list
// should return. Defaults to CURRENT so the primary admin screen never mixes retired 7-day/test
// products into the live 7-product catalog - see TuitionPromotionCatalog.
public enum TuitionCatalogScope {
    CURRENT,
    HISTORICAL,
    ALL
}
