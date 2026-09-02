import type { CustomerStatus } from "../../types/api";
import "./StatusBadge.css";

const LABELS: Record<CustomerStatus, string> = {
  ACTIVE: "Active",
  SUSPENDED: "Suspended",
  DISABLED: "Disabled",
};

const TONES: Record<CustomerStatus, string> = {
  ACTIVE: "green",
  SUSPENDED: "amber",
  DISABLED: "red",
};

interface CustomerStatusBadgeProps {
  status: CustomerStatus;
}

export function CustomerStatusBadge({ status }: CustomerStatusBadgeProps) {
  return <span className={`status-badge status-badge--${TONES[status]}`}>{LABELS[status]}</span>;
}
