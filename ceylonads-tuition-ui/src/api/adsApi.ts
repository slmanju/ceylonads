import { apiClient } from "./apiClient";
import type { AdResponse, AdSearchParams, CreateAdRequest, PageResponse } from "../types/api";

export async function searchAds(params: AdSearchParams = {}): Promise<PageResponse<AdResponse>> {
  const { attributeFilters, ...rest } = params;
  const { data } = await apiClient.get<PageResponse<AdResponse>>("/api/ads", {
    params: { ...rest, ...attributeFilters },
  });
  return data;
}

export async function getAd(id: number | string): Promise<AdResponse> {
  const { data } = await apiClient.get<AdResponse>(`/api/ads/${id}`);
  return data;
}

export async function getMyAds(): Promise<AdResponse[]> {
  const { data } = await apiClient.get<AdResponse[]>("/api/ads/mine");
  return data;
}

export async function getFeaturedAds(limit = 8): Promise<AdResponse[]> {
  const { data } = await apiClient.get<AdResponse[]>("/api/ads/featured", { params: { limit } });
  return data;
}

// Category-scoped featured slot (e.g. TUITION_FEATURED), distinct from the homepage-wide
// getFeaturedAds above — this is what makes "Featured Classes" genuinely tuition-specific.
export async function getCategoryFeaturedAds(categorySlug: string, limit = 8): Promise<AdResponse[]> {
  const { data } = await apiClient.get<AdResponse[]>("/api/ads/category-featured", {
    params: { categorySlug, limit },
  });
  return data;
}

export async function createAd(payload: CreateAdRequest): Promise<AdResponse> {
  const { data } = await apiClient.post<AdResponse>("/api/ads", payload);
  return data;
}

export async function updateAd(id: number | string, payload: CreateAdRequest): Promise<AdResponse> {
  const { data } = await apiClient.put<AdResponse>(`/api/ads/${id}`, payload);
  return data;
}

export async function deactivateAd(id: number | string): Promise<void> {
  await apiClient.delete(`/api/ads/${id}`);
}
