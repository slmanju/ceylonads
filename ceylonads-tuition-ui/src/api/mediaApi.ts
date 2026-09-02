import { apiClient } from "./apiClient";
import type { MediaResponse } from "../types/api";

export async function uploadAdMedia(
  adId: number | string,
  file: File,
  onProgress?: (percent: number) => void,
): Promise<MediaResponse> {
  const formData = new FormData();
  formData.append("file", file);

  const { data } = await apiClient.post<MediaResponse>(`/api/ads/${adId}/media`, formData, {
    onUploadProgress: (event) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded / event.total) * 100));
      }
    },
  });
  return data;
}
