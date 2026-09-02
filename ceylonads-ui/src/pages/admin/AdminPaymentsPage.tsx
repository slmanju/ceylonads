import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as adminPaymentApi from "../../api/adminPaymentApi";
import { AdminPageHeader } from "../../components/AdminPageHeader/AdminPageHeader";
import { AdminPaymentReviewModal } from "../../components/AdminPaymentReviewModal/AdminPaymentReviewModal";
import { PaymentStatusBadge } from "../../components/PaymentStatusBadge/PaymentStatusBadge";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { useToast } from "../../components/Toast/ToastProvider";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatPrice } from "../../utils/formatPrice";
import { formatDate } from "../../utils/formatDate";
import type { PaymentResponse, PaymentSummaryResponse, PaymentStatus } from "../../types/api";
import "./AdminPaymentsPage.css";

type FilterTab = "ALL" | PaymentStatus;

const FILTERS: { key: FilterTab; label: string }[] = [
  { key: "SUBMITTED", label: "Submitted" },
  { key: "PENDING", label: "Pending" },
  { key: "APPROVED", label: "Approved" },
  { key: "REJECTED", label: "Rejected" },
  { key: "CANCELLED", label: "Cancelled" },
  { key: "ALL", label: "All" },
];

export function AdminPaymentsPage() {
  const { showToast } = useToast();
  const [payments, setPayments] = useState<PaymentSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // Default focus is on payments waiting for admin review, per the Phase 3 workflow.
  const [tab, setTab] = useState<FilterTab>("SUBMITTED");
  const [reviewingId, setReviewingId] = useState<number | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    return adminPaymentApi
      .listPayments()
      .then(setPayments)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load payments.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const visible = tab === "ALL" ? payments : payments.filter((p) => p.status === tab);

  const handleChanged = (updated: PaymentResponse) => {
    setPayments((prev) =>
      prev.map((p) =>
        p.id === updated.id
          ? {
              ...p,
              status: updated.status,
              bankReference: updated.bankReference,
              submittedAt: updated.submittedAt,
            }
          : p,
      ),
    );
    showToast(updated.status === "APPROVED" ? "Payment approved and promotion activated." : `Payment ${updated.status.toLowerCase()}.`);
  };

  return (
    <div className="admin-payments-page">
      <AdminPageHeader title="Payments" subtitle="Review submitted bank transfers. Approving a payment activates its promotion." />

      {!loading && !error && payments.length > 0 && (
        <div className="admin-payments-page__tabs">
          {FILTERS.map(({ key, label }) => (
            <button
              key={key}
              type="button"
              className={`admin-payments-page__tab ${tab === key ? "admin-payments-page__tab--active" : ""}`}
              onClick={() => setTab(key)}
            >
              {label} ({key === "ALL" ? payments.length : payments.filter((p) => p.status === key).length})
            </button>
          ))}
        </div>
      )}

      {loading && <LoadingState label="Loading payments…" />}

      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && payments.length === 0 && <EmptyState title="No payments yet." />}

      {!loading && !error && payments.length > 0 && visible.length === 0 && (
        <EmptyState title="No payments in this status." />
      )}

      {!loading && !error && visible.length > 0 && (
        <div className="admin-payments-page__table-wrap">
          <table className="admin-payments-page__table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Customer</th>
                <th>Ad</th>
                <th>Plan</th>
                <th>Amount</th>
                <th>Bank Reference</th>
                <th>Submitted</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visible.map((payment) => (
                <tr key={payment.id}>
                  <td className="admin-payments-page__reference">{payment.paymentReference}</td>
                  <td>{payment.customerDisplayName}</td>
                  <td>
                    <Link to={`/ads/${payment.adId}`}>{payment.adTitle}</Link>
                  </td>
                  <td>{payment.promotionPlanName}</td>
                  <td>{formatPrice(payment.amount)}</td>
                  <td>{payment.bankReference ?? "—"}</td>
                  <td>{formatDate(payment.submittedAt)}</td>
                  <td>
                    <PaymentStatusBadge status={payment.status} />
                  </td>
                  <td>
                    <button type="button" className="btn btn-primary" onClick={() => setReviewingId(payment.id)}>
                      {payment.status === "SUBMITTED" || payment.status === "PENDING" ? "Review" : "View"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <AdminPaymentReviewModal paymentId={reviewingId} onClose={() => setReviewingId(null)} onChanged={handleChanged} />
    </div>
  );
}
