import { apiClient } from "./apiClient";
import type {
  CompatiblePromotionPlanResponse,
  CreatePromotionRequest,
  PromotionBannerResponse,
  PromotionPlanResponse,
  PromotionResponse,
} from "../types/api";

export async function listActivePromotionPlans(): Promise<PromotionPlanResponse[]> {
  const { data } = await apiClient.get<PromotionPlanResponse[]>("/api/promotion-plans");
  return data;
}

export async function getCompatiblePromotionPlans(adId: number | string): Promise<CompatiblePromotionPlanResponse[]> {
  const { data } = await apiClient.get<CompatiblePromotionPlanResponse[]>(`/api/promotions/compatible-plans/${adId}`);
  return data;
}

export async function getActiveBanners(slotCode: string): Promise<PromotionBannerResponse[]> {
  const { data } = await apiClient.get<PromotionBannerResponse[]>(`/api/promotion-slots/code/${slotCode}/active-banners`);
  return data;
}

export async function createPromotion(payload: CreatePromotionRequest): Promise<PromotionResponse> {
  const { data } = await apiClient.post<PromotionResponse>("/api/promotions", payload);
  return data;
}

export async function getMyPromotions(): Promise<PromotionResponse[]> {
  const { data } = await apiClient.get<PromotionResponse[]>("/api/promotions/me");
  return data;
}

export async function getPromotion(id: number | string): Promise<PromotionResponse> {
  const { data } = await apiClient.get<PromotionResponse>(`/api/promotions/${id}`);
  return data;
}

export async function cancelPromotion(id: number | string): Promise<PromotionResponse> {
  const { data } = await apiClient.post<PromotionResponse>(`/api/promotions/${id}/cancel`);
  return data;
}
