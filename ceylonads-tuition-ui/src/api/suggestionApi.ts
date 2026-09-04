import { apiClient } from "./apiClient";
import type { SuggestionCreateRequest } from "../types/api";

// POST /api/tuition/suggestions is public - no auth header is required, though apiClient sends
// one automatically if the visitor happens to be logged in (harmless either way).
export async function createSuggestion(payload: SuggestionCreateRequest): Promise<void> {
  await apiClient.post("/api/tuition/suggestions", payload);
}
