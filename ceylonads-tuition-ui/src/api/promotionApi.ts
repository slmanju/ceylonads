import { apiClient } from "./apiClient";
import type { CompatiblePromotionPlanResponse, PromotionResponse, TuitionCampaignResponse } from "../types/api";

// The Tuition UI's storefront campaign banner/modal - GET /api/tuition/promotions/campaign. 204
// (no active, customer-visible Tuition campaign right now) resolves to null; the caller decides
// what "no campaign" means for rendering, this function only talks to the one backend endpoint.
export async function getActiveTuitionCampaign(): Promise<TuitionCampaignResponse | null> {
  const response = await apiClient.get<TuitionCampaignResponse>("/api/tuition/promotions/campaign", {
    validateStatus: (status) => status === 200 || status === 204,
  });
  return response.status === 204 ? null : response.data;
}

// The Tuition UI's dedicated promotion API (see TuitionSellerPromotionController) - TUITION-channel
// only, with backend-resolved pricing/availability. Never falls back to the generic
// /api/promotion-plans or /api/promotions/** endpoints, which could mix in MAIN_SITE/BOARDING
// products or promotions from the same shared account.
export async function listTuitionPromotionPlans(): Promise<CompatiblePromotionPlanResponse[]> {
  const { data } = await apiClient.get<CompatiblePromotionPlanResponse[]>("/api/tuition/promotions/plans");
  return data;
}

// My Classes' "Promote" action's plan-selection step. Ad-eligibility filtered (category, live slot
// availability) - see TuitionMyClassPromotionController - narrower than listTuitionPromotionPlans's
// general catalog.
export async function getCompatibleTuitionPromotionPlans(adId: number | string): Promise<CompatiblePromotionPlanResponse[]> {
  const { data } = await apiClient.get<CompatiblePromotionPlanResponse[]>(`/api/tuition/my-classes/${adId}/promotion-plans`);
  return data;
}

export async function createTuitionPromotion(adId: number | string, promotionPlanId: number): Promise<PromotionResponse> {
  const { data } = await apiClient.post<PromotionResponse>("/api/tuition/promotions", { adId, promotionPlanId });
  return data;
}

// Used to reflect each class's current promotion state on My Classes (Promote / Manage Promotion /
// Promote Again) - TUITION-scoped, so a tutor who also sells on MAIN_SITE/BOARDING under the same
// account never sees those promotions mixed in here.
export async function getMyPromotions(): Promise<PromotionResponse[]> {
  const { data } = await apiClient.get<PromotionResponse[]>("/api/tuition/promotions/my");
  return data;
}
