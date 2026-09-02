import { apiClient } from "./apiClient";
import type { AdResponse } from "../types/api";

// Shared ad-moderation boundary for MODERATOR + ADMIN (see backend SecurityConfig /api/moderation/**).
// Used by AdminAdsPage/AdminAdReviewPage, which are reused at both /admin/ads (Admin) and
// /moderation (Moderator + Admin) so a Moderator's session hits an endpoint it's authorized for.
export async function listPendingAds(): Promise<AdResponse[]> {
  const { data } = await apiClient.get<AdResponse[]>("/api/moderation/ads/pending");
  return data;
}

export async function approveAd(id: number | string): Promise<AdResponse> {
  const { data } = await apiClient.patch<AdResponse>(`/api/moderation/ads/${id}/approve`);
  return data;
}

export async function rejectAd(id: number | string): Promise<AdResponse> {
  const { data } = await apiClient.patch<AdResponse>(`/api/moderation/ads/${id}/reject`);
  return data;
}

export async function deactivateAd(id: number | string): Promise<AdResponse> {
  const { data } = await apiClient.patch<AdResponse>(`/api/moderation/ads/${id}/deactivate`);
  return data;
}
