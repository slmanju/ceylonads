import { apiClient } from "./apiClient";
import type {
  AdminCreatePromotionRequest,
  AdminPromotionPlanRequest,
  AdminPromotionPlanUpdateRequest,
  MediaResponse,
  PromotionPlanResponse,
  PromotionResponse,
  PromotionSlotAdminRequest,
  PromotionSlotResponse,
  PromotionSlotUpdateRequest,
  PromotionSlotUsageResponse,
  PromotionStatus,
} from "../types/api";

export async function listAllPromotionPlans(): Promise<PromotionPlanResponse[]> {
  const { data } = await apiClient.get<PromotionPlanResponse[]>("/api/admin/promotion-plans");
  return data;
}

export async function createPromotionPlan(payload: AdminPromotionPlanRequest): Promise<PromotionPlanResponse> {
  const { data } = await apiClient.post<PromotionPlanResponse>("/api/admin/promotion-plans", payload);
  return data;
}

export async function updatePromotionPlan(
  id: number | string,
  payload: AdminPromotionPlanUpdateRequest,
): Promise<PromotionPlanResponse> {
  const { data } = await apiClient.put<PromotionPlanResponse>(`/api/admin/promotion-plans/${id}`, payload);
  return data;
}

export async function activatePromotionPlan(id: number | string): Promise<PromotionPlanResponse> {
  const { data } = await apiClient.patch<PromotionPlanResponse>(`/api/admin/promotion-plans/${id}/activate`);
  return data;
}

export async function deactivatePromotionPlan(id: number | string): Promise<PromotionPlanResponse> {
  const { data } = await apiClient.patch<PromotionPlanResponse>(`/api/admin/promotion-plans/${id}/deactivate`);
  return data;
}

export async function listPromotions(status?: PromotionStatus): Promise<PromotionResponse[]> {
  const { data } = await apiClient.get<PromotionResponse[]>("/api/admin/promotions", {
    params: status ? { status } : undefined,
  });
  return data;
}

export async function activatePromotion(id: number | string): Promise<PromotionResponse> {
  const { data } = await apiClient.patch<PromotionResponse>(`/api/admin/promotions/${id}/activate`);
  return data;
}

export async function approvePromotion(id: number | string): Promise<PromotionResponse> {
  const { data } = await apiClient.patch<PromotionResponse>(`/api/admin/promotions/${id}/approve`);
  return data;
}

export async function cancelPromotionAsAdmin(id: number | string): Promise<PromotionResponse> {
  const { data } = await apiClient.patch<PromotionResponse>(`/api/admin/promotions/${id}/cancel`);
  return data;
}

export async function uploadBannerMedia(file: File): Promise<MediaResponse> {
  const form = new FormData();
  form.append("file", file);
  const { data } = await apiClient.post<MediaResponse>("/api/admin/promotions/banner-media", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

export async function createPromotion(payload: AdminCreatePromotionRequest): Promise<PromotionResponse> {
  const { data } = await apiClient.post<PromotionResponse>("/api/admin/promotions", payload);
  return data;
}

export async function listPromotionSlots(): Promise<PromotionSlotResponse[]> {
  const { data } = await apiClient.get<PromotionSlotResponse[]>("/api/admin/promotion-slots");
  return data;
}

export async function createPromotionSlot(payload: PromotionSlotAdminRequest): Promise<PromotionSlotResponse> {
  const { data } = await apiClient.post<PromotionSlotResponse>("/api/admin/promotion-slots", payload);
  return data;
}

export async function updatePromotionSlot(
  id: number | string,
  payload: PromotionSlotUpdateRequest,
): Promise<PromotionSlotResponse> {
  const { data } = await apiClient.put<PromotionSlotResponse>(`/api/admin/promotion-slots/${id}`, payload);
  return data;
}

export async function activatePromotionSlot(id: number | string): Promise<PromotionSlotResponse> {
  const { data } = await apiClient.patch<PromotionSlotResponse>(`/api/admin/promotion-slots/${id}/activate`);
  return data;
}

export async function deactivatePromotionSlot(id: number | string): Promise<PromotionSlotResponse> {
  const { data } = await apiClient.patch<PromotionSlotResponse>(`/api/admin/promotion-slots/${id}/deactivate`);
  return data;
}

export async function getPromotionSlotUsage(id: number | string): Promise<PromotionSlotUsageResponse> {
  const { data } = await apiClient.get<PromotionSlotUsageResponse>(`/api/admin/promotion-slots/${id}/usage`);
  return data;
}
