import { TUITION_PROMOTIONS } from "../data/promotion.mock";
import type {
  DetailPromotionContext,
  DetailPromotions,
  HomepagePromotions,
  ProfilePromotionContext,
  SearchPromotionContext,
  SearchPromotions,
  TuitionPromotion,
  TuitionPromotionPlacement,
} from "../model/promotion";
import type { TuitionPromotionRepository } from "./tuitionPromotionRepository";
import { byDisplayOrder, isActive, isEligible, type PromotionMatchContext } from "./matching";

// Density caps - see tuition-promotion spec section "PROMOTION DENSITY". Keep these as the single
// source of truth rather than letting each page component decide how many promotions to render.
const MAX_HOME_FEATURED = 5;
const MAX_PROFILE_PROMOTIONS = 3;

function activeOfType(placementType: TuitionPromotionPlacement): TuitionPromotion[] {
  return TUITION_PROMOTIONS.filter((p) => p.placementType === placementType && isActive(p)).sort(byDisplayOrder);
}

function firstEligible(candidates: TuitionPromotion[], context: PromotionMatchContext): TuitionPromotion | undefined {
  return candidates.find((p) => isEligible(p.eligibility, context));
}

function detailContextToMatch(context: DetailPromotionContext): PromotionMatchContext {
  return {
    subjects: context.subjectLabel ? [context.subjectLabel] : undefined,
    levels: context.level ? [context.level] : undefined,
    curriculums: context.curriculum ? [context.curriculum] : undefined,
    deliveryModes: context.deliveryModes,
    locationSlugs: context.locationSlugs,
    profileType: context.profileType,
  };
}

function searchContextToMatch(context: SearchPromotionContext): PromotionMatchContext {
  return {
    subjects: context.subjects,
    levels: context.levels,
    curriculums: context.curriculums,
    deliveryModes: context.deliveryModes,
    locationSlugs: context.locationSlug ? [context.locationSlug] : undefined,
  };
}

export class MockTuitionPromotionRepository implements TuitionPromotionRepository {
  async getHomepagePromotions(): Promise<HomepagePromotions> {
    return {
      topBanner: activeOfType("TUITION_HOME_TOP_BANNER")[0],
      featured: activeOfType("TUITION_HOME_FEATURED").slice(0, MAX_HOME_FEATURED),
      sidebarTop: activeOfType("TUITION_HOME_SIDEBAR_TOP")[0],
      sidebarMiddle: activeOfType("TUITION_HOME_SIDEBAR_MIDDLE")[0],
      sidebarBottom: activeOfType("TUITION_HOME_SIDEBAR_BOTTOM")[0],
    };
  }

  async getSearchPromotions(context: SearchPromotionContext): Promise<SearchPromotions> {
    const match = searchContextToMatch(context);

    return {
      topBanner: firstEligible(activeOfType("TUITION_SEARCH_TOP_BANNER"), match),
      sidebarTop: firstEligible(activeOfType("TUITION_SEARCH_SIDEBAR_TOP"), match),
      sidebarMiddle: firstEligible(activeOfType("TUITION_SEARCH_SIDEBAR_MIDDLE"), match),
      sidebarBottom: firstEligible(activeOfType("TUITION_SEARCH_SIDEBAR_BOTTOM"), match),
    };
  }

  async getDetailPromotions(context: DetailPromotionContext): Promise<DetailPromotions> {
    const match = detailContextToMatch(context);

    return {
      side: firstEligible(activeOfType("TUITION_DETAIL_SIDE"), match),
      banner: firstEligible(activeOfType("TUITION_DETAIL_BANNER"), match),
    };
  }

  async getProfilePromotions(context: ProfilePromotionContext): Promise<TuitionPromotion[]> {
    const match: PromotionMatchContext = {
      subjects: context.subjectLabel ? [context.subjectLabel] : undefined,
      levels: context.level ? [context.level] : undefined,
      profileType: context.profileType,
    };

    return TUITION_PROMOTIONS.filter((p) => p.eligibility?.profileType === context.profileType && isActive(p) && isEligible(p.eligibility, match))
      .sort(byDisplayOrder)
      .slice(0, MAX_PROFILE_PROMOTIONS);
  }
}
