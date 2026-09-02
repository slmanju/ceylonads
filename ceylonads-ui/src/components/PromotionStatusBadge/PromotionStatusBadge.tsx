import type { PromotionStatus } from "../../types/api";
import "./PromotionStatusBadge.css";

const LABELS: Record<PromotionStatus, string> = {
  PENDING_PAYMENT: "Pending payment",
  PENDING_APPROVAL: "Awaiting approval",
  ACTIVE: "Active",
  EXPIRED: "Expired",
  CANCELLED: "Cancelled",
};

const TONES: Record<PromotionStatus, string> = {
  PENDING_PAYMENT: "amber",
  PENDING_APPROVAL: "amber",
  ACTIVE: "green",
  EXPIRED: "gray",
  CANCELLED: "red",
};

interface PromotionStatusBadgeProps {
  status: PromotionStatus;
}

export function PromotionStatusBadge({ status }: PromotionStatusBadgeProps) {
  return <span className={`promotion-status-badge promotion-status-badge--${TONES[status]}`}>{LABELS[status]}</span>;
}
