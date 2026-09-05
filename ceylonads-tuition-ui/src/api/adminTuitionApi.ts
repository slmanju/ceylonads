import { apiClient } from "./apiClient";
import type {
  AdResponse,
  AdStatus,
  PromotionResponse,
  SuggestionStatus,
  TuitionAdminDashboardSummary,
  TuitionSuggestionAdmin,
} from "../types/api";

// Every call below hits /api/admin/tuition/** (ROLE_ADMIN-only, see SecurityConfig on the
// backend) and is always scoped to SourceChannel.TUITION server-side - never MAIN_SITE.

export async function getDashboardSummary(): Promise<TuitionAdminDashboardSummary> {
  const { data } = await apiClient.get<TuitionAdminDashboardSummary>("/api/admin/tuition/dashboard");
  return data;
}

export async function getPendingTuitionAds(): Promise<AdResponse[]> {
  const { data } = await apiClient.get<AdResponse[]>("/api/admin/tuition/pending");
  return data;
}

// Generalized form of getPendingTuitionAds above - backs the Classes page's
// Pending/Active/Rejected/Expired tabs.
export async function getTuitionAdsByStatus(status: AdStatus): Promise<AdResponse[]> {
  const { data } = await apiClient.get<AdResponse[]>("/api/admin/tuition/ads", { params: { status } });
  return data;
}

export async function getTuitionAd(id: number | string): Promise<AdResponse> {
  const { data } = await apiClient.get<AdResponse>(`/api/admin/tuition/ads/${id}`);
  return data;
}

export async function approveTuitionAd(id: number | string): Promise<AdResponse> {
  const { data } = await apiClient.patch<AdResponse>(`/api/admin/tuition/ads/${id}/approve`);
  return data;
}

export async function rejectTuitionAd(id: number | string): Promise<AdResponse> {
  const { data } = await apiClient.patch<AdResponse>(`/api/admin/tuition/ads/${id}/reject`);
  return data;
}

// Admin-initiated "Promote Class" - creates a real Promotion via the shared promotion domain and
// activates it immediately (the admin's action here is itself the approval). Only the plan is
// sent; price/duration/campaign/owner are all resolved server-side.
export async function promoteTuitionAd(id: number | string, promotionPlanId: number): Promise<PromotionResponse> {
  const { data } = await apiClient.post<PromotionResponse>(`/api/admin/tuition/ads/${id}/promotions`, { promotionPlanId });
  return data;
}

export async function listSuggestions(): Promise<TuitionSuggestionAdmin[]> {
  const { data } = await apiClient.get<TuitionSuggestionAdmin[]>("/api/admin/tuition/suggestions");
  return data;
}

export async function getSuggestion(id: number | string): Promise<TuitionSuggestionAdmin> {
  const { data } = await apiClient.get<TuitionSuggestionAdmin>(`/api/admin/tuition/suggestions/${id}`);
  return data;
}

export async function updateSuggestionStatus(
  id: number | string,
  status: SuggestionStatus,
): Promise<TuitionSuggestionAdmin> {
  const { data } = await apiClient.patch<TuitionSuggestionAdmin>(
    `/api/admin/tuition/suggestions/${id}/status`,
    null,
    { params: { status } },
  );
  return data;
}
