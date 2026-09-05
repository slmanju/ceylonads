import { FaArrowUp, FaBullhorn, FaColumns } from "react-icons/fa";
import type { IconType } from "react-icons";
import type { PromotionPlanResponse } from "../types/api";

// Customer-facing presentation for the seven current ezClass Tuition promotion products (see
// ceylonads-api's TuitionPromotionCatalog). Plan codes/slot codes/prices/durations are stable
// backend identifiers and stay untouched - this only maps them to the plain-language names,
// placement wording, and benefit copy shown to customers, since the backend `name`/`slotName`
// fields still carry the older internal "Featured"/"Spotlight" wording used by the admin console.
export interface PromotionDisplay {
  displayName: string;
  whereItAppears: string;
  benefit: string;
  icon: IconType;
  recommended?: boolean;
}

const PROMOTION_DISPLAY_BY_CODE: Record<string, PromotionDisplay> = {
  TUITION_SEARCH_BOOST_30D: {
    displayName: "Search Boost",
    whereItAppears: "Inside Matching Search Results",
    benefit: "Your class appears higher than normal matching classes when it matches what a student searches for.",
    icon: FaArrowUp,
    recommended: true,
  },
  TUITION_SEARCH_TOP_30D: {
    displayName: "Search Page Top",
    whereItAppears: "Top of Search Page",
    benefit: "Always visible before students browse search results.",
    icon: FaBullhorn,
  },
  TUITION_SEARCH_SIDEBAR_TOP_30D: {
    displayName: "Search Page Right Side",
    whereItAppears: "Right of Search Results",
    benefit: "Your class poster appears while students browse search results.",
    icon: FaColumns,
  },
  TUITION_HOME_FEATURED_30D: {
    displayName: "Homepage Top",
    whereItAppears: "Top of Homepage",
    benefit: "Your class appears prominently in the homepage promotion carousel.",
    icon: FaBullhorn,
  },
  TUITION_HOME_LATEST_RIGHT_30D: {
    displayName: "Homepage Right Side",
    whereItAppears: "Beside Latest Classes",
    benefit: "Your class poster appears while students browse the newest classes.",
    icon: FaColumns,
  },
  TUITION_DETAIL_TOP_30D: {
    displayName: "Class Page Top",
    whereItAppears: "Top of Class Pages",
    benefit: "Students viewing other classes can discover your class before reading the details.",
    icon: FaBullhorn,
  },
  TUITION_DETAIL_RIGHT_30D: {
    displayName: "Class Page Right Side",
    whereItAppears: "Right of Class Pages",
    benefit: "Your class poster appears while students view another class.",
    icon: FaColumns,
  },
};

// Commercial priority order (search first - it's ezClass's main discovery surface), not database
// display_order. Any plan code not in this list (e.g. a future product) sorts after all of these.
const DISPLAY_ORDER = Object.keys(PROMOTION_DISPLAY_BY_CODE);

// Falls back to the backend's own name/slotName/description for any plan code not in the map
// above, so an unmapped future plan still renders something sensible instead of blank text.
export function getPromotionDisplay(plan: PromotionPlanResponse): PromotionDisplay {
  return (
    PROMOTION_DISPLAY_BY_CODE[plan.code] ?? {
      displayName: plan.name,
      whereItAppears: plan.slotName,
      benefit: plan.description,
      icon: FaBullhorn,
    }
  );
}

// Lighter-weight lookup for contexts that only have a plan code and the backend's own name on
// hand (e.g. a PromotionResponse's promotionPlanCode/promotionPlanName) rather than a full
// PromotionPlanResponse - a promotion "My Classes" summary list, or an admin plan <select>.
export function getPromotionDisplayName(code: string, fallbackName: string): string {
  return PROMOTION_DISPLAY_BY_CODE[code]?.displayName ?? fallbackName;
}

export function sortByPromotionDisplayOrder<T>(items: T[], getCode: (item: T) => string): T[] {
  return [...items].sort((a, b) => {
    const aIndex = DISPLAY_ORDER.indexOf(getCode(a));
    const bIndex = DISPLAY_ORDER.indexOf(getCode(b));
    if (aIndex === -1 && bIndex === -1) return 0;
    if (aIndex === -1) return 1;
    if (bIndex === -1) return -1;
    return aIndex - bIndex;
  });
}
