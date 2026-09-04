package com.slmanju.ceylonads.tuition;

import java.util.Set;

// The ezClass Tuition storefront's current, business-supported promotion catalog - the seven
// placements actually offered on ezClass today (Search Page Featured, Search Boost, Search Page
// Spotlight, Homepage Featured, Homepage Spotlight, Detail Page Featured, Detail Page Spotlight).
// Distinct from "every TUITION-channel row in the shared promotion_plans/promotion_slots tables",
// which also includes retired 7-day products and disabled sidebar-middle/bottom test slots kept
// only for historical audit (old promotions still reference them).
//
// A plan is "current" when its slot is one of these seven AND the plan itself is active - not
// identified by a fixed list of plan codes, so a new admin-created plan on an already-supported
// slot (e.g. a future 60-day variant of Search Boost) is automatically current without needing a
// code update here. Today that resolves to exactly the seven live 30-day products:
// TUITION_SEARCH_TOP_30D, TUITION_SEARCH_BOOST_30D, TUITION_SEARCH_SIDEBAR_TOP_30D,
// TUITION_HOME_FEATURED_30D, TUITION_HOME_LATEST_RIGHT_30D, TUITION_DETAIL_TOP_30D,
// TUITION_DETAIL_RIGHT_30D.
//
// Consulted only by the Tuition admin console (AdminTuitionPromotionPlanController/
// AdminTuitionPromotionCampaignController/AdminTuitionAdsController's dashboard) - never by the
// shared PromotionService or the customer-facing Tuition storefront, which continue to key off
// the plan/slot `active` flag exactly as before.
//
// Deliberately a small hardcoded allowlist, not a DB column: the *set of supported placements*
// only changes when ezClass actually launches a new placement type, which is a code change/deploy
// anyway - unlike price/campaign dates/plan active state, which stay fully admin-editable (see
// CLAUDE.md's Flyway-vs-admin-managed-data rule).
public final class TuitionPromotionCatalog {

    public static final Set<String> CURRENT_SLOT_CODES = Set.of(
            "TUITION_SEARCH_TOP",
            "TUITION_SEARCH_BOOST",
            "TUITION_SEARCH_SIDEBAR_TOP",
            "TUITION_FEATURED",
            "TUITION_HOME_LATEST_RIGHT",
            "TUITION_DETAIL_TOP_CAROUSEL",
            "TUITION_DETAIL_RIGHT");

    public static boolean isCurrentPlan(String slotCode, boolean planActive) {
        return planActive && CURRENT_SLOT_CODES.contains(slotCode);
    }

    private TuitionPromotionCatalog() {
    }
}
