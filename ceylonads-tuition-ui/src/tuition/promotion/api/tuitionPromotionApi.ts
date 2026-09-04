import { apiClient } from "../../../api/apiClient";
import type { TuitionFeaturedCardResponse, TuitionPromotionResponse, TuitionPromotionsResponse } from "../../../types/api";
import type {
  DetailPromotionContext,
  DetailPromotions,
  HomepagePromotions,
  PromotionLabel,
  PromotionTarget,
  ProfilePromotionContext,
  SearchPromotionContext,
  SearchPromotions,
  TuitionPromotion,
  TuitionPromotionPlacement,
} from "../model/promotion";
import { MockTuitionPromotionRepository } from "./mockTuitionPromotionRepository";
import type { TuitionPromotionRepository } from "./tuitionPromotionRepository";

function toTarget(response: TuitionPromotionResponse): PromotionTarget {
  return response.targetType === "AD"
    ? { type: "AD", adSlug: response.adSlug ?? undefined }
    : { type: "EXTERNAL", url: response.targetUrl ?? undefined };
}

// The backend has no title/subtitle for banner-kind promotions (see Promotion entity - a banner
// is just an image + link), so a banner card's title falls back to its badge text. Sidebar cards
// are ad-backed and always carry the ad's real title.
function toTuitionPromotion(response: TuitionPromotionResponse): TuitionPromotion {
  return {
    id: String(response.id),
    placementType: response.slot as TuitionPromotionPlacement,
    label: response.badge as PromotionLabel,
    title: response.title ?? response.badge,
    subtitle: response.subtitle ?? undefined,
    imageUrl: response.imageUrl ?? undefined,
    target: toTarget(response),
    ctaLabel: response.ctaLabel ?? undefined,
    displayOrder: response.displayOrder,
  };
}

function firstOrUndefined(list: TuitionPromotionResponse[]): TuitionPromotion | undefined {
  const [first] = list;
  return first ? toTuitionPromotion(first) : undefined;
}

// Adapts a real TuitionFeaturedCardResponse (GET /api/tuition/featured?slot=...) into the same
// TuitionPromotion shape PromotionSideCard/SpotlightPosterTile render, so the Homepage Spotlight
// (TUITION_HOME_LATEST_RIGHT, a 4-visible vertical carousel - see HomeSpotlightRail), Detail Right
// (TUITION_DETAIL_RIGHT, single-card), and Search Page Spotlight (TUITION_SEARCH_SIDEBAR_TOP, a
// 12-advertiser slot - see SearchSpotlightRail) placements can reuse the same card presentation
// without a parallel component per placement. `placementType` is carried through for completeness
// only; no renderer reads it (see model/promotion.ts). `label` defaults to "FEATURED"
// (Homepage/Detail Spotlight); Search Page Spotlight passes "PROMOTED" instead, to read
// consistently with the rest of the search page's paid inventory - though SpotlightPosterTile
// itself never renders the label at all, only PromotionSideCard does.
export function featuredCardToPromotion(
  card: TuitionFeaturedCardResponse,
  placementType: Extract<TuitionPromotionPlacement, "TUITION_HOME_LATEST_RIGHT" | "TUITION_DETAIL_RIGHT" | "TUITION_SEARCH_SIDEBAR_TOP">,
  label: PromotionLabel = "FEATURED",
): TuitionPromotion {
  const subtitle = [card.subject, card.level, card.primaryLocation?.name]
    .filter((value): value is string => Boolean(value))
    .join(" · ");
  return {
    id: String(card.id),
    placementType,
    label,
    title: card.title,
    subtitle: subtitle || undefined,
    imageUrl: card.primaryImageUrl ?? undefined,
    target: { type: "AD", adSlug: card.slug },
    ctaLabel: "View Class",
    price: card.price ?? undefined,
    displayOrder: 0,
  };
}

// Placeholder for when tuition-specific promotion placements move into the real CeylonAds
// promotion backend. getSearchPromotions below has a real endpoint (GET /api/tuition/promotions)
// and is wired up unconditionally further down; the other methods have no backend yet.
class HttpTuitionPromotionRepository implements TuitionPromotionRepository {
  // No backend endpoint exists yet for the homepage's mock-only placements (topBanner etc).
  // Returning empty (rather than mock data or throwing) so the homepage's top banner always
  // renders its PromotionBannerSelfAd fallback until a real endpoint is wired up here. Homepage
  // Spotlight (TUITION_HOME_LATEST_RIGHT) is unrelated - it's a real slot fetched separately via
  // useFeaturedTuition, not through this method.
  async getHomepagePromotions(): Promise<HomepagePromotions> {
    return { featured: [] };
  }

  async getSearchPromotions(_context: SearchPromotionContext): Promise<SearchPromotions> {
    const { data } = await apiClient.get<TuitionPromotionsResponse>("/api/tuition/promotions");
    return {
      topBanner: firstOrUndefined(data.topBanner),
      sidebarTop: firstOrUndefined(data.sidebarTop),
      sidebarMiddle: firstOrUndefined(data.sidebarMiddle),
      sidebarBottom: firstOrUndefined(data.sidebarBottom),
    };
  }

  async getDetailPromotions(_context: DetailPromotionContext): Promise<DetailPromotions> {
    throw new Error("HttpTuitionPromotionRepository is not implemented yet - no backend endpoint exists.");
  }

  async getProfilePromotions(_context: ProfilePromotionContext): Promise<TuitionPromotion[]> {
    throw new Error("HttpTuitionPromotionRepository is not implemented yet - no backend endpoint exists.");
  }
}

// Single composition point for the detail/profile placements, which have no real backend yet.
// Set VITE_TUITION_PROMOTION_DATA_SOURCE=real once they do - everything else in the app depends
// only on the TuitionPromotionRepository interface.
const useMockPromotionApi = (import.meta.env.VITE_TUITION_PROMOTION_DATA_SOURCE as string | undefined) !== "real";

const decorativeRepository: TuitionPromotionRepository = useMockPromotionApi
  ? new MockTuitionPromotionRepository()
  : new HttpTuitionPromotionRepository();
const httpTuitionPromotionRepository = new HttpTuitionPromotionRepository();

// getHomepagePromotions and getSearchPromotions both go straight to Http, independent of the
// mock/real toggle above: getSearchPromotions has a real, always-available backend
// (GET /api/tuition/promotions), and getHomepagePromotions has no mock inventory left to fall
// back to (see promotion.mock.ts) - it deliberately returns empty until a real endpoint exists,
// so the homepage always shows real data or its own Advertise Here fallback, never mock content.
export const tuitionPromotionRepository: TuitionPromotionRepository = {
  getHomepagePromotions: () => httpTuitionPromotionRepository.getHomepagePromotions(),
  getSearchPromotions: (context) => httpTuitionPromotionRepository.getSearchPromotions(context),
  getDetailPromotions: (context) => decorativeRepository.getDetailPromotions(context),
  getProfilePromotions: (context) => decorativeRepository.getProfilePromotions(context),
};
