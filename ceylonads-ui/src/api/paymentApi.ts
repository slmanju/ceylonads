import { apiClient } from "./apiClient";
import type { BankTransferDetailsResponse, PaymentResponse, PaymentSummaryResponse, SubmitPaymentRequest } from "../types/api";

export async function getBankTransferDetails(): Promise<BankTransferDetailsResponse> {
  const { data } = await apiClient.get<BankTransferDetailsResponse>("/api/payments/bank-transfer-details");
  return data;
}

export async function getMyPayments(): Promise<PaymentSummaryResponse[]> {
  const { data } = await apiClient.get<PaymentSummaryResponse[]>("/api/payments/me");
  return data;
}

export async function getPayment(id: number | string): Promise<PaymentResponse> {
  const { data } = await apiClient.get<PaymentResponse>(`/api/payments/${id}`);
  return data;
}

export async function uploadPaymentReceipt(
  id: number | string,
  file: File,
  onProgress?: (percent: number) => void,
): Promise<PaymentResponse> {
  const formData = new FormData();
  formData.append("file", file);

  const { data } = await apiClient.post<PaymentResponse>(`/api/payments/${id}/receipt`, formData, {
    onUploadProgress: (event) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded / event.total) * 100));
      }
    },
  });
  return data;
}

export async function submitPayment(id: number | string, payload: SubmitPaymentRequest): Promise<PaymentResponse> {
  const { data } = await apiClient.post<PaymentResponse>(`/api/payments/${id}/submit`, payload);
  return data;
}

export async function cancelPayment(id: number | string): Promise<PaymentResponse> {
  const { data } = await apiClient.post<PaymentResponse>(`/api/payments/${id}/cancel`);
  return data;
}
