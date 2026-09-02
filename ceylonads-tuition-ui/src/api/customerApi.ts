import { apiClient } from "./apiClient";
import type { CustomerResponse } from "../types/api";

export async function getMyProfile(): Promise<CustomerResponse> {
  const { data } = await apiClient.get<CustomerResponse>("/api/customers/me");
  return data;
}
