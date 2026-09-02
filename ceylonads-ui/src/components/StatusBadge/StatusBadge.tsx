import type { AdStatus } from "../../types/api";
import "./StatusBadge.css";

const LABELS: Record<AdStatus, string> = {
  DRAFT: "Draft",
  PENDING_REVIEW: "Pending review",
  ACTIVE: "Active",
  REJECTED: "Rejected",
  SOLD: "Sold",
  EXPIRED: "Expired",
  DEACTIVATED: "Deactivated",
};

const TONES: Record<AdStatus, string> = {
  DRAFT: "neutral",
  PENDING_REVIEW: "amber",
  ACTIVE: "green",
  REJECTED: "red",
  SOLD: "slate",
  EXPIRED: "gray",
  DEACTIVATED: "gray",
};

interface StatusBadgeProps {
  status: AdStatus;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  return <span className={`status-badge status-badge--${TONES[status]}`}>{LABELS[status]}</span>;
}
