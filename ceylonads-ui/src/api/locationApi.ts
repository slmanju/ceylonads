import { apiClient } from "./apiClient";
import type { AdminLocationRequest, LocationResponse } from "../types/api";

export async function listLocations(): Promise<LocationResponse[]> {
  const { data } = await apiClient.get<LocationResponse[]>("/api/locations");
  return data;
}

export async function createLocation(payload: AdminLocationRequest): Promise<LocationResponse> {
  const { data } = await apiClient.post<LocationResponse>("/api/admin/locations", payload);
  return data;
}
