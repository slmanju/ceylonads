import type { PaymentStatus } from "../../types/api";
import "./PaymentStatusBadge.css";

const LABELS: Record<PaymentStatus, string> = {
  PENDING: "Pending",
  SUBMITTED: "Submitted",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
};

const TONES: Record<PaymentStatus, string> = {
  PENDING: "amber",
  SUBMITTED: "blue",
  APPROVED: "green",
  REJECTED: "red",
  CANCELLED: "gray",
};

interface PaymentStatusBadgeProps {
  status: PaymentStatus;
  /** Overrides the default label while keeping the status's colour, e.g. "Awaiting Verification"
   * in place of "Submitted" when this badge is shown alongside a promotion instead of a payment. */
  label?: string;
}

export function PaymentStatusBadge({ status, label }: PaymentStatusBadgeProps) {
  return <span className={`payment-status-badge payment-status-badge--${TONES[status]}`}>{label ?? LABELS[status]}</span>;
}
