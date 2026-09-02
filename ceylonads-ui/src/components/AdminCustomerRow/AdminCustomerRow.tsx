import { FaEnvelope, FaPhone, FaUserCircle } from "react-icons/fa";
import type { CustomerResponse } from "../../types/api";
import { CustomerStatusBadge } from "../StatusBadge/CustomerStatusBadge";
import "./AdminCustomerRow.css";

interface AdminCustomerRowProps {
  customer: CustomerResponse;
  busy?: boolean;
  onSuspend: (customer: CustomerResponse) => void;
  onActivate: (customer: CustomerResponse) => void;
}

export function AdminCustomerRow({ customer, busy, onSuspend, onActivate }: AdminCustomerRowProps) {
  return (
    <div className="admin-customer-row">
      <div className="admin-customer-row__identity">
        <FaUserCircle aria-hidden="true" className="admin-customer-row__avatar" />
        <div>
          <p className="admin-customer-row__name">{customer.displayName}</p>
          <p className="admin-customer-row__username">@{customer.username}</p>
        </div>
      </div>

      <div className="admin-customer-row__contact">
        <span>
          <FaEnvelope aria-hidden="true" /> {customer.email}
        </span>
        {customer.phone && (
          <span>
            <FaPhone aria-hidden="true" /> {customer.phone}
          </span>
        )}
      </div>

      <div className="admin-customer-row__status">
        <CustomerStatusBadge status={customer.status} />
      </div>

      <div className="admin-customer-row__actions">
        {customer.status === "ACTIVE" && (
          <button
            type="button"
            className="btn btn-outline admin-customer-row__action"
            disabled={busy}
            onClick={() => onSuspend(customer)}
          >
            Suspend
          </button>
        )}
        {(customer.status === "SUSPENDED" || customer.status === "DISABLED") && (
          <button
            type="button"
            className="btn btn-primary admin-customer-row__action"
            disabled={busy}
            onClick={() => onActivate(customer)}
          >
            Activate
          </button>
        )}
      </div>
    </div>
  );
}
