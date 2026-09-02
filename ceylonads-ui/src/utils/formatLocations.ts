import type { LocationResponse } from "../types/api";

// Ads have 0..N locations. Returns null for zero (online tuition, remote services, etc.) so
// callers can omit the location row entirely instead of rendering "Location: " or a bare
// separator; callers that already show other location-implying context (e.g. Class Mode) should
// prefer that when this is null.
export function formatAdLocations(locations: LocationResponse[]): string | null {
  if (locations.length === 0) return null;
  return locations.map((l) => l.name).join(", ");
}
