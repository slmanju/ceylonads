import type { PaymentMethod } from "../types/api";

const LABELS: Record<PaymentMethod, string> = {
  BANK_TRANSFER: "Bank Transfer",
  CASH: "Cash",
  OTHER: "Other",
};

export function formatPaymentMethod(method: PaymentMethod): string {
  return LABELS[method];
}
