import type { AdStatus } from "../../types/api";
import "./StatusBadge.css";

const LABELS: Record<AdStatus, string> = {
  DRAFT: "Draft",
  PENDING_REVIEW: "Pending Review",
  ACTIVE: "Active",
  REJECTED: "Rejected",
  SOLD: "Filled",
  EXPIRED: "Expired",
  DEACTIVATED: "Deactivated",
};

export function StatusBadge({ status }: { status: AdStatus }) {
  return <span className={`status-badge status-badge--${status.toLowerCase()}`}>{LABELS[status]}</span>;
}
