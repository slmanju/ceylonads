import type {
  AdResponse,
  LocationResponse,
  PageResponse,
  TuitionClassCardResponse,
  TuitionClassDetailResponse,
  TuitionFeaturedCardResponse,
  TuitionFilterMetadataResponse,
} from "../../types/api";
import type { TuitionDetails } from "../model/tuition";

// Query options for getFeaturedTuition. `slot` reads a specific, independently-sellable
// TUITION_FEATURED-shaped slot by its exact code (e.g. TUITION_DETAIL_TOP_CAROUSEL for the class
// detail page's top carousel) instead of the default homepage/search TUITION_FEATURED slot.
// `excludeAdId` drops one ad (e.g. the listing currently being viewed) from the result - see
// ceylonads-api's TuitionFeaturedService.
export interface FeaturedTuitionQuery {
  size?: number;
  slot?: string;
  excludeAdId?: number;
}

// The UI must not know whether tuition data comes from the mock provider or the real CeylonAds
// backend - both MockTuitionRepository and HttpTuitionRepository implement this same contract.
// See tuitionApi.ts for the single composition point that picks between them.
export interface TuitionRepository {
  // Mock-only decorative enrichment (schedule/home-visit/teacher profile) layered onto card grids
  // elsewhere in the app (ClassesPage, OnlineClassesPage, TutorsPage) - unrelated to the two
  // methods below.
  getDetails(ad: AdResponse, locations: LocationResponse[]): Promise<TuitionDetails>;
  getDetailsMap(ads: AdResponse[], locations: LocationResponse[]): Promise<Map<number, TuitionDetails>>;

  // The class detail page's single source of truth: GET /api/tuition/classes/{slug} in real mode.
  // `signal` lets callers abort a stale in-flight request (e.g. StrictMode's mount/unmount/remount,
  // or the slug changing before the previous fetch resolves).
  getClassDetail(slug: string, signal?: AbortSignal): Promise<TuitionClassDetailResponse>;
  // Small "Similar Classes" rail: GET /api/tuition/classes/{slug}/similar?size= in real mode.
  getSimilarClasses(slug: string, size?: number, signal?: AbortSignal): Promise<TuitionClassCardResponse[]>;

  // Fixed TUITION_FEATURED-shaped carousels: GET /api/tuition/featured?size=&slot=&excludeAdId= in
  // real mode. Backs the homepage/search "Featured Tuition" carousel (default slot) and the class
  // detail page's top carousel (slot: TUITION_DETAIL_TOP_CAROUSEL) alike. Isolated from the
  // generic /api/ads/category-featured endpoint that backs the "Featured Classes" section above it
  // on the homepage - see ceylonads-api's TuitionFeaturedService.
  getFeaturedTuition(query?: FeaturedTuitionQuery, signal?: AbortSignal): Promise<TuitionFeaturedCardResponse[]>;

  // Homepage "Latest Classes" paginated feed: GET /api/tuition/classes?page=&size= in real mode.
  // Isolated from the generic /api/ads search endpoint - see ceylonads-api's
  // TuitionClassService.getLatest.
  getLatestClasses(page?: number, size?: number, signal?: AbortSignal): Promise<PageResponse<TuitionClassCardResponse>>;

  // Filter panel master data: GET /api/tuition/filters in real mode. Not yet wired into any
  // filter UI - see TuitionFilterMetadataResponse for the response shape.
  getFilters(signal?: AbortSignal): Promise<TuitionFilterMetadataResponse>;
}
