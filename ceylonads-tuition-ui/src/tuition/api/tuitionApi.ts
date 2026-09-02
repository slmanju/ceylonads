import { apiClient } from "../../api/apiClient";
import type {
  AdResponse,
  AdSearchParams,
  LocationResponse,
  PageResponse,
  TuitionClassCardResponse,
  TuitionClassCreateRequest,
  TuitionClassDetailResponse,
  TuitionFeaturedCardResponse,
  TuitionFilterMetadataResponse,
} from "../../types/api";
import type { TuitionDetails } from "../model/tuition";
import { MockTuitionRepository } from "./mockTuitionRepository";
import type { FeaturedTuitionQuery, TuitionRepository } from "./tuitionRepository";

// getDetails/getDetailsMap remain a placeholder for when the decorative mock-only metadata
// (schedule/home-visit/teacher profile) moves into the real backend - no such endpoint exists
// today, so real mode simply never resolves that enrichment for card grids. getClassDetail/
// getSimilarClasses below call the real, already-implemented tuition endpoints.
class HttpTuitionRepository implements TuitionRepository {
  async getDetails(ad: AdResponse, _locations: LocationResponse[]): Promise<TuitionDetails> {
    const { data } = await apiClient.get<TuitionDetails>(`/api/tuition/ads/${ad.id}/details`);
    return data;
  }

  async getDetailsMap(ads: AdResponse[], locations: LocationResponse[]): Promise<Map<number, TuitionDetails>> {
    const entries = await Promise.all(ads.map(async (ad) => [ad.id, await this.getDetails(ad, locations)] as const));
    return new Map(entries);
  }

  async getClassDetail(slug: string, signal?: AbortSignal): Promise<TuitionClassDetailResponse> {
    const { data } = await apiClient.get<TuitionClassDetailResponse>(`/api/tuition/classes/${slug}`, { signal });
    return data;
  }

  async getSimilarClasses(slug: string, size = 3, signal?: AbortSignal): Promise<TuitionClassCardResponse[]> {
    const { data } = await apiClient.get<TuitionClassCardResponse[]>(`/api/tuition/classes/${slug}/similar`, {
      params: { size },
      signal,
    });
    return data;
  }

  async getFeaturedTuition(
    { size = 10, slot, excludeAdId }: FeaturedTuitionQuery = {},
    signal?: AbortSignal,
  ): Promise<TuitionFeaturedCardResponse[]> {
    const { data } = await apiClient.get<TuitionFeaturedCardResponse[]>("/api/tuition/featured", {
      params: { size, slot, excludeAdId },
      signal,
    });
    return data;
  }

  async getLatestClasses(
    page = 0,
    size = 6,
    signal?: AbortSignal,
  ): Promise<PageResponse<TuitionClassCardResponse>> {
    const { data } = await apiClient.get<PageResponse<TuitionClassCardResponse>>("/api/tuition/classes", {
      params: { page, size },
      signal,
    });
    return data;
  }

  async getFilters(signal?: AbortSignal): Promise<TuitionFilterMetadataResponse> {
    const { data } = await apiClient.get<TuitionFilterMetadataResponse>("/api/tuition/filters", { signal });
    return data;
  }
}

// Filtered/paginated search for the Classes/Tutors/Online Classes pages: GET
// /api/tuition/classes/search. Deliberately NOT the generic searchAds()/`/api/ads` - that endpoint
// is scoped server-side to SourceChannel.MAIN_SITE only and never returns Tuition listings, no
// matter what category/attribute filters are passed. Same params/response shape as searchAds so
// callers migrating off it don't need to change anything else.
export async function searchTuitionClasses(params: AdSearchParams = {}): Promise<PageResponse<AdResponse>> {
  const { attributeFilters, ...rest } = params;
  const { data } = await apiClient.get<PageResponse<AdResponse>>("/api/tuition/classes/search", {
    params: { ...rest, ...attributeFilters },
  });
  return data;
}

// Tuition class lifecycle (Post Ad wizard): POST/PUT /api/tuition/classes - deliberately not the
// generic createAd()/updateAd() (`/api/ads`), which would tag the ad SourceChannel.MAIN_SITE and
// make it invisible to every tuition read endpoint above (search, latest, featured, similar).
export async function createTuitionClass(payload: TuitionClassCreateRequest): Promise<TuitionClassDetailResponse> {
  const { data } = await apiClient.post<TuitionClassDetailResponse>("/api/tuition/classes", payload);
  return data;
}

export async function updateTuitionClass(
  id: number | string,
  payload: TuitionClassCreateRequest,
): Promise<TuitionClassDetailResponse> {
  const { data } = await apiClient.put<TuitionClassDetailResponse>(`/api/tuition/classes/${id}`, payload);
  return data;
}

// Single composition point: this is the only place that chooses between mock and real tuition
// data. Set VITE_TUITION_DATA_SOURCE=real once a backend endpoint exists for the decorative
// schedule/home-visit/teacher data - everything else in the app depends only on the
// TuitionRepository interface.
const useMockTuitionApi = (import.meta.env.VITE_TUITION_DATA_SOURCE as string | undefined) !== "real";

const decorativeRepository: TuitionRepository = useMockTuitionApi ? new MockTuitionRepository() : new HttpTuitionRepository();
const httpTuitionRepository = new HttpTuitionRepository();

// getClassDetail/getSimilarClasses/getFeaturedTuition/getLatestClasses have a real,
// always-available backend (GET /api/tuition/classes/{slug}, .../similar, GET /api/tuition/featured,
// and GET /api/tuition/classes) independent of the mock/real toggle above, so they always go
// straight to the real endpoints rather than through MockTuitionRepository's generic-/api/ads
// stand-in.
export const tuitionRepository: TuitionRepository = {
  getDetails: (ad, locations) => decorativeRepository.getDetails(ad, locations),
  getDetailsMap: (ads, locations) => decorativeRepository.getDetailsMap(ads, locations),
  getClassDetail: (slug, signal) => httpTuitionRepository.getClassDetail(slug, signal),
  getSimilarClasses: (slug, size, signal) => httpTuitionRepository.getSimilarClasses(slug, size, signal),
  getFeaturedTuition: (query, signal) => httpTuitionRepository.getFeaturedTuition(query, signal),
  getLatestClasses: (page, size, signal) => httpTuitionRepository.getLatestClasses(page, size, signal),
  getFilters: (signal) => httpTuitionRepository.getFilters(signal),
};
