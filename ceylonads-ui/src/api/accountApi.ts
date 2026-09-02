import { apiClient } from "./apiClient";
import type { ChangePasswordRequest, ChangePasswordResponse } from "../types/api";

export async function changePassword(payload: ChangePasswordRequest): Promise<ChangePasswordResponse> {
  const { data } = await apiClient.put<ChangePasswordResponse>("/api/account/password", payload);
  return data;
}
