import { apiClient } from "./apiClient";
import type {
  PaymentResponse,
  PaymentSummaryResponse,
  PaymentStatus,
  RejectPaymentRequest,
  VerifyPaymentRequest,
} from "../types/api";

export async function listPayments(status?: PaymentStatus): Promise<PaymentSummaryResponse[]> {
  const { data } = await apiClient.get<PaymentSummaryResponse[]>("/api/admin/payments", {
    params: status ? { status } : undefined,
  });
  return data;
}

export async function countPayments(status: PaymentStatus): Promise<number> {
  const { data } = await apiClient.get<{ count: number }>("/api/admin/payments/count", { params: { status } });
  return data.count;
}

export async function getPayment(id: number | string): Promise<PaymentResponse> {
  const { data } = await apiClient.get<PaymentResponse>(`/api/admin/payments/${id}`);
  return data;
}

export async function approvePayment(id: number | string, payload?: VerifyPaymentRequest): Promise<PaymentResponse> {
  const { data } = await apiClient.post<PaymentResponse>(`/api/admin/payments/${id}/approve`, payload);
  return data;
}

export async function rejectPayment(id: number | string, payload: RejectPaymentRequest): Promise<PaymentResponse> {
  const { data } = await apiClient.post<PaymentResponse>(`/api/admin/payments/${id}/reject`, payload);
  return data;
}
