import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMyPayments } from "../api/paymentApi";
import { PaymentStatusBadge } from "../components/PaymentStatusBadge/PaymentStatusBadge";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { EmptyState } from "../components/EmptyState/EmptyState";
import type { PaymentSummaryResponse } from "../types/api";
import { formatPrice } from "../utils/formatPrice";
import { formatDate } from "../utils/formatDate";
import { formatPaymentMethod } from "../utils/formatPaymentMethod";
import { getApiErrorMessage } from "../utils/apiError";
import "./MyPaymentsPage.css";

export function MyPaymentsPage() {
  const [payments, setPayments] = useState<PaymentSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    return getMyPayments()
      .then(setPayments)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load your payments.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="my-payments-page container">
      <div className="my-payments-page__header">
        <h1>My Payments</h1>
        <Link to="/my-promotions" className="btn btn-secondary">
          Back to My Promotions
        </Link>
      </div>

      {loading && <LoadingState label="Loading your payments…" />}

      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && payments.length === 0 && (
        <EmptyState
          title="You don't have any payments yet."
          message="Payments are created automatically when you promote an ad."
          action={
            <Link to="/my-ads" className="btn btn-primary">
              Go to My Ads
            </Link>
          }
        />
      )}

      {!loading && !error && payments.length > 0 && (
        <div className="my-payments-page__table-wrap">
          <table className="my-payments-page__table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Ad</th>
                <th>Plan</th>
                <th>Amount</th>
                <th>Method</th>
                <th>Status</th>
                <th>Date</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {payments.map((payment) => (
                <tr key={payment.id}>
                  <td className="my-payments-page__reference">{payment.paymentReference}</td>
                  <td>
                    <Link to={`/ads/${payment.adId}`}>{payment.adTitle}</Link>
                  </td>
                  <td>{payment.promotionPlanName}</td>
                  <td>{formatPrice(payment.amount)}</td>
                  <td>{formatPaymentMethod(payment.paymentMethod)}</td>
                  <td>
                    <PaymentStatusBadge status={payment.status} />
                  </td>
                  <td>{formatDate(payment.submittedAt ?? payment.createdAt)}</td>
                  <td>
                    <Link to={`/my-payments/${payment.id}`} className="btn btn-outline">
                      View
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
