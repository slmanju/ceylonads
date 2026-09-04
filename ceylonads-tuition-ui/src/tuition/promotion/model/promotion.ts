// Frontend-only tuition promotion domain model. These placements do not exist in the shared
// CeylonAds promotion backend yet (see tuition CLAUDE.md "Promotions") - this is a prototyping
// layer behind TuitionPromotionRepository so the real backend integration can replace only the
// provider implementation later without touching any page/component. Never import this module's
// mock data or matching logic directly from a page - go through the repository/hooks.

import type { Curriculum, DeliveryMode, TeacherProfileType, TuitionLevel } from "../../model/tuition";

// TUITION_HOME_LATEST_RIGHT, TUITION_DETAIL_RIGHT, and TUITION_SEARCH_SIDEBAR_TOP (the "Search
// Page Spotlight" product - see tuitionPromotionApi.ts and ceylonads-api's V22 migration) are real
// ceylonads-api promotion_slots codes (see featuredCardToPromotion below) - every other value here
// is mock-only, with no backend counterpart. TUITION_SEARCH_SIDEBAR_MIDDLE/BOTTOM are the same
// real slot family but stay retired/unused in this catalog (only one fixed right-side search
// position exists), so they're kept here only for type completeness, never fetched.
export type TuitionPromotionPlacement =
  | "TUITION_HOME_TOP_BANNER"
  | "TUITION_HOME_FEATURED"
  | "TUITION_HOME_SIDEBAR_TOP"
  | "TUITION_HOME_SIDEBAR_MIDDLE"
  | "TUITION_HOME_SIDEBAR_BOTTOM"
  | "TUITION_SEARCH_TOP_BANNER"
  | "TUITION_SEARCH_SIDEBAR_TOP"
  | "TUITION_SEARCH_SIDEBAR_MIDDLE"
  | "TUITION_SEARCH_SIDEBAR_BOTTOM"
  | "TUITION_DETAIL_SIDE"
  | "TUITION_DETAIL_BANNER"
  | "TUITION_HOME_LATEST_RIGHT"
  | "TUITION_DETAIL_RIGHT";

// PROMOTED is used for search-results-page placements (Search Page Spotlight) to read consistently
// with the rest of the search page's paid inventory (see SearchPromoCard), rather than FEATURED's
// homepage/detail-page wording. Search Boost's own PROMOTED badge is unrelated to this type - it's
// a plain ClassCard badge driven by AdResponse.promoted, not a PromotionTarget-based placement.
export type PromotionLabel = "SPONSORED" | "FEATURED" | "PROMOTED";

export type PromotionTargetType = "AD" | "TEACHER_PROFILE" | "INSTITUTE_PROFILE" | "EXTERNAL";

export interface PromotionTarget {
  type: PromotionTargetType;
  /** AD / TEACHER_PROFILE / INSTITUTE_PROFILE: slug of the real CeylonAds ad that represents this
   *  tutor/institute - resolves to /classes/:slug, the closest live page until a dedicated tutor/
   *  institute profile route exists. */
  adSlug?: string;
  /** EXTERNAL: an app-relative or external URL (e.g. a filtered /classes search, or a campaign page). */
  url?: string;
}

/** Optional eligibility metadata. Every present dimension must match (AND across dimensions,
 *  OR within a dimension's list) for a promotion to be considered eligible for a search/detail
 *  context - see matching.ts. A promotion with no eligibility at all is broadly eligible. */
export interface PromotionEligibility {
  /** Matched case-insensitively as a substring against the context's subject/title text. */
  subjects?: string[];
  levels?: TuitionLevel[];
  curriculums?: Curriculum[];
  deliveryModes?: DeliveryMode[];
  /** Location slugs (district/city), matched against the context's location. */
  locationSlugs?: string[];
  profileType?: TeacherProfileType;
}

export interface TuitionPromotion {
  id: string;
  placementType: TuitionPromotionPlacement;
  label: PromotionLabel;
  title: string;
  subtitle?: string;
  imageUrl?: string;
  target: PromotionTarget;
  ctaLabel?: string;
  /** Monthly/class fee, shown on compact card presentations (e.g. Search Page Spotlight) that
   *  have room to surface it. Omit to hide the price line entirely. */
  price?: number;
  /** Lower sorts first within a placement. */
  displayOrder: number;
  /** ISO date (yyyy-MM-dd). Omit for an always-active promotion. */
  activeFrom?: string;
  /** ISO date (yyyy-MM-dd). Omit for an always-active promotion. */
  activeTo?: string;
  eligibility?: PromotionEligibility;
}

export interface SearchPromotionContext {
  categorySlug?: string;
  locationSlug?: string;
  levels?: TuitionLevel[];
  curriculums?: Curriculum[];
  deliveryModes?: DeliveryMode[];
  /** Free-text search query and/or resolved subject labels from the current result set. */
  subjects?: string[];
}

export interface DetailPromotionContext {
  categorySlug?: string;
  subjectLabel?: string;
  level?: TuitionLevel;
  curriculum?: Curriculum;
  deliveryModes?: DeliveryMode[];
  locationSlugs?: string[];
  profileType?: TeacherProfileType;
}

export interface ProfilePromotionContext {
  profileType: TeacherProfileType;
  subjectLabel?: string;
  level?: TuitionLevel;
}

// The homepage renders its sidebar promotions the same way the search page does (see
// SearchPromotions/PromotionSidebar) - a dedicated, clearly-labelled advertisement rail next to
// the organic "Latest Classes" grid, never mixed into it. Distinct placement codes
// (TUITION_HOME_SIDEBAR_*) from the search page's TUITION_SEARCH_SIDEBAR_* so the two contexts can
// carry different promotion inventory even though they share a rendering pattern.
export interface HomepagePromotions {
  topBanner?: TuitionPromotion;
  featured: TuitionPromotion[];
  sidebarTop?: TuitionPromotion;
  sidebarMiddle?: TuitionPromotion;
  sidebarBottom?: TuitionPromotion;
}

// The search page renders promotions in a dedicated sidebar column (see PromotionSidebar) plus an
// optional full-width banner above the results row - never mixed into the organic result grid, so
// none of these count toward organic pagination/result totals (see ClassSearchResults).
export interface SearchPromotions {
  topBanner?: TuitionPromotion;
  sidebarTop?: TuitionPromotion;
  sidebarMiddle?: TuitionPromotion;
  sidebarBottom?: TuitionPromotion;
}

export interface DetailPromotions {
  side?: TuitionPromotion;
  banner?: TuitionPromotion;
}
