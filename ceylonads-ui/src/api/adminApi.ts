import { apiClient } from "./apiClient";
import type { AdResponse, CustomerResponse, CustomerStatus } from "../types/api";

// Ad moderation (pending/approve/reject/deactivate) lives in moderationApi.ts - it's shared with
// MODERATOR, not ADMIN-only like the rest of this module.

export async function listCustomers(): Promise<CustomerResponse[]> {
  const { data } = await apiClient.get<CustomerResponse[]>("/api/admin/customers");
  return data;
}

export async function listCustomerActiveAds(customerId: number | string): Promise<AdResponse[]> {
  const { data } = await apiClient.get<AdResponse[]>(`/api/admin/customers/${customerId}/ads`);
  return data;
}

export async function updateCustomerStatus(
  id: number | string,
  status: CustomerStatus,
): Promise<CustomerResponse> {
  const { data } = await apiClient.patch<CustomerResponse>(`/api/admin/customers/${id}/status`, null, {
    params: { status },
  });
  return data;
}
