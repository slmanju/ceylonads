import axios from "axios";
import { getToken, clearAuth } from "../auth/authStorage";
import { emitAuthCleared } from "../auth/authEvents";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

// In dev, requests go through the Vite proxy (see vite.config.ts) same-origin, because the
// backend does not send CORS headers for cross-origin XHR/fetch. Direct <img> loads aren't
// affected by this, so media URLs stay absolute.
const axiosBaseUrl = import.meta.env.DEV ? "" : API_BASE_URL;

export const apiClient = axios.create({
  baseURL: axiosBaseUrl,
});

apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuth();
      emitAuthCleared();
    }
    return Promise.reject(error);
  },
);

export function resolveMediaUrl(url: string): string {
  if (/^https?:\/\//i.test(url)) {
    return url;
  }
  return `${API_BASE_URL}${url.startsWith("/") ? "" : "/"}${url}`;
}
