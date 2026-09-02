import type { LocationResponse } from "../types/api";

// Ads have 0..N locations. Returns null for zero (online-only classes) so callers can omit the
// location row entirely instead of rendering an empty placeholder.
export function formatAdLocations(locations: LocationResponse[]): string | null {
  if (locations.length === 0) return null;
  return locations.map((l) => l.name).join(", ");
}
