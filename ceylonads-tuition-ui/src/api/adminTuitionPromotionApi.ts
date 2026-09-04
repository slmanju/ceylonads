import { apiClient } from "./apiClient";
import type {
  AdminPromotionCampaignRequest,
  AdminPromotionCampaignUpdateRequest,
  AdminPromotionPlanRequest,
  AdminPromotionPlanUpdateRequest,
  PromotionCampaignResponse,
  PromotionPlanResponse,
  PromotionResponse,
  PromotionSlotResponse,
  PromotionStatus,
  TuitionCatalogScope,
} from "../types/api";

// Every call below hits /api/admin/tuition/** (ROLE_ADMIN-only, see SecurityConfig on the
// backend) and is always scoped to SourceChannel.TUITION server-side - never MAIN_SITE/BOARDING.

export async function listPromotions(status?: PromotionStatus): Promise<PromotionResponse[]> {
  const { data } = await apiClient.get<PromotionResponse[]>("/api/admin/tuition/promotions", {
    params: status ? { status } : undefined,
  });
  return data;
}

export async function getPromotion(id: number | string): Promise<PromotionResponse> {
  const { data } = await apiClient.get<PromotionResponse>(`/api/admin/tuition/promotions/${id}`);
  return data;
}

export async function approvePromotion(id: number | string): Promise<PromotionResponse> {
  const { data } = await apiClient.patch<PromotionResponse>(`/api/admin/tuition/promotions/${id}/approve`);
  return data;
}

export async function rejectPromotion(id: number | string): Promise<PromotionResponse> {
  const { data } = await apiClient.patch<PromotionResponse>(`/api/admin/tuition/promotions/${id}/reject`);
  return data;
}

// scope defaults to CURRENT server-side (the seven live ezClass products) - pass "HISTORICAL" or
// "ALL" explicitly to see retired/test products kept only for audit. Never fetch "ALL" and filter
// client-side for the main list; the backend already enforces this scoping.
export async function listPromotionPlans(scope?: TuitionCatalogScope): Promise<PromotionPlanResponse[]> {
  const { data } = await apiClient.get<PromotionPlanResponse[]>("/api/admin/tuition/promotion-plans", {
    params: scope ? { scope } : undefined,
  });
  return data;
}

// Read-only - feeds the create-plan slot picker, already restricted server-side to the seven
// current supported placements. No Tuition slot CRUD exists.
export async function listPlanSlots(): Promise<PromotionSlotResponse[]> {
  const { data } = await apiClient.get<PromotionSlotResponse[]>("/api/admin/tuition/promotion-plans/slots");
  return data;
}

// scope=ALL so a historical plan (reached e.g. via a direct campaign-mapping lookup) can still be
// resolved even though it's excluded from the default current-catalog list.
export async function getPromotionPlan(id: number | string): Promise<PromotionPlanResponse> {
  const plans = await listPromotionPlans("ALL");
  const plan = plans.find((p) => String(p.id) === String(id));
  if (!plan) throw new Error("Promotion plan not found");
  return plan;
}

export async function createPromotionPlan(payload: AdminPromotionPlanRequest): Promise<PromotionPlanResponse> {
  const { data } = await apiClient.post<PromotionPlanResponse>("/api/admin/tuition/promotion-plans", payload);
  return data;
}

export async function updatePromotionPlan(
  id: number | string,
  payload: AdminPromotionPlanUpdateRequest,
): Promise<PromotionPlanResponse> {
  const { data } = await apiClient.put<PromotionPlanResponse>(`/api/admin/tuition/promotion-plans/${id}`, payload);
  return data;
}

export async function activatePromotionPlan(id: number | string): Promise<PromotionPlanResponse> {
  const { data } = await apiClient.patch<PromotionPlanResponse>(`/api/admin/tuition/promotion-plans/${id}/activate`);
  return data;
}

export async function deactivatePromotionPlan(id: number | string): Promise<PromotionPlanResponse> {
  const { data } = await apiClient.patch<PromotionPlanResponse>(`/api/admin/tuition/promotion-plans/${id}/deactivate`);
  return data;
}

export async function listCampaigns(): Promise<PromotionCampaignResponse[]> {
  const { data } = await apiClient.get<PromotionCampaignResponse[]>("/api/admin/tuition/campaigns");
  return data;
}

export async function getCampaign(id: number | string): Promise<PromotionCampaignResponse> {
  const campaigns = await listCampaigns();
  const campaign = campaigns.find((c) => String(c.id) === String(id));
  if (!campaign) throw new Error("Promotion campaign not found");
  return campaign;
}

export async function createCampaign(payload: AdminPromotionCampaignRequest): Promise<PromotionCampaignResponse> {
  const { data } = await apiClient.post<PromotionCampaignResponse>("/api/admin/tuition/campaigns", payload);
  return data;
}

export async function updateCampaign(
  id: number | string,
  payload: AdminPromotionCampaignUpdateRequest,
): Promise<PromotionCampaignResponse> {
  const { data } = await apiClient.put<PromotionCampaignResponse>(`/api/admin/tuition/campaigns/${id}`, payload);
  return data;
}

export async function activateCampaign(id: number | string): Promise<PromotionCampaignResponse> {
  const { data } = await apiClient.patch<PromotionCampaignResponse>(`/api/admin/tuition/campaigns/${id}/activate`);
  return data;
}

export async function deactivateCampaign(id: number | string): Promise<PromotionCampaignResponse> {
  const { data } = await apiClient.patch<PromotionCampaignResponse>(`/api/admin/tuition/campaigns/${id}/deactivate`);
  return data;
}
