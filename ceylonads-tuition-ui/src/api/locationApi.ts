import { apiClient } from "./apiClient";
import type { LocationResponse } from "../types/api";

export async function listLocations(): Promise<LocationResponse[]> {
  const { data } = await apiClient.get<LocationResponse[]>("/api/locations");
  return data;
}
