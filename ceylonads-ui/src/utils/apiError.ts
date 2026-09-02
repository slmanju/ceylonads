import { isAxiosError } from "axios";
import type { ApiErrorBody } from "../types/api";

export function getApiErrorMessage(error: unknown, fallback = "Something went wrong. Please try again."): string {
  if (isAxiosError<ApiErrorBody>(error)) {
    const body = error.response?.data;
    if (body?.message) return body.message;
    if (body?.errors) {
      const first = Object.values(body.errors)[0];
      if (typeof first === "string") return first;
    }
    if (error.response?.status === 401) return "Invalid credentials. Please try again.";
    if (error.response?.status === 403) return "You don't have permission to do that.";
    if (!error.response) return "Could not reach the server. Please check your connection.";
  }
  return fallback;
}
