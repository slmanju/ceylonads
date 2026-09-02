import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMyPromotions } from "../api/promotionApi";
import { getMyPayments } from "../api/paymentApi";
import { PromotionStatusBadge } from "../components/PromotionStatusBadge/PromotionStatusBadge";
import { PaymentStatusBadge } from "../components/PaymentStatusBadge/PaymentStatusBadge";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { EmptyState } from "../components/EmptyState/EmptyState";
import type { PaymentSummaryResponse, PromotionResponse } from "../types/api";
import { formatPrice } from "../utils/formatPrice";
import { formatDate } from "../utils/formatDate";
import { getApiErrorMessage } from "../utils/apiError";
import "./MyPromotionsPage.css";

export function MyPromotionsPage() {
  const [promotions, setPromotions] = useState<PromotionResponse[]>([]);
  const [payments, setPayments] = useState<PaymentSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    return Promise.all([getMyPromotions(), getMyPayments()])
      .then(([promotionData, paymentData]) => {
        setPromotions(promotionData);
        setPayments(paymentData);
      })
      .catch((err) => setError(getApiErrorMessage(err, "Could not load your promotions.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const paymentForPromotion = (promotionId: number) => payments.find((p) => p.promotionId === promotionId);

  return (
    <div className="my-promotions-page container">
      <div className="my-promotions-page__header">
        <h1>My Promotions</h1>
        <div className="my-promotions-page__header-actions">
          <Link to="/my-promotions/request" className="btn btn-primary">
            + Request Promotion
          </Link>
          <Link to="/my-payments" className="btn btn-secondary">
            My Payments
          </Link>
          <Link to="/my-ads" className="btn btn-secondary">
            Back to My Ads
          </Link>
        </div>
      </div>

      {loading && <LoadingState label="Loading your promotions…" />}

      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && promotions.length === 0 && (
        <EmptyState
          title="You haven't promoted any ads yet."
          message="Promote an active ad to feature it on the homepage, its category page, or search results."
          action={
            <Link to="/my-ads" className="btn btn-primary">
              Go to My Ads
            </Link>
          }
        />
      )}

      {!loading && !error && promotions.length > 0 && (
        <div className="my-promotions-page__table-wrap">
          <table className="my-promotions-page__table">
            <thead>
              <tr>
                <th>Ad</th>
                <th>Plan</th>
                <th>Price</th>
                <th>Status</th>
                <th>Created</th>
                <th>Starts</th>
                <th>Ends</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {promotions.map((promotion) => {
                const payment = promotion.status === "PENDING_PAYMENT" ? paymentForPromotion(promotion.id) : undefined;

                return (
                  <tr key={promotion.id}>
                    <td>
                      {promotion.kind === "BANNER_PROMOTION" || !promotion.adId ? (
                        <span>Banner ({promotion.slotCode})</span>
                      ) : (
                        <Link to={`/ads/${promotion.adId}`}>{promotion.adTitle}</Link>
                      )}
                    </td>
                    <td>{promotion.promotionPlanName}</td>
                    <td>{formatPrice(promotion.price)}</td>
                    <td>
                      {promotion.status !== "PENDING_PAYMENT" || !payment ? (
                        <PromotionStatusBadge status={promotion.status} />
                      ) : payment.status === "SUBMITTED" ? (
                        <PaymentStatusBadge status={payment.status} label="Awaiting Verification" />
                      ) : payment.status === "REJECTED" ? (
                        <PaymentStatusBadge status={payment.status} label="Payment Rejected" />
                      ) : (
                        <PaymentStatusBadge status={payment.status} label="Pending Payment" />
                      )}
                    </td>
                    <td>{formatDate(promotion.createdAt)}</td>
                    <td>{formatDate(promotion.startsAt)}</td>
                    <td>{formatDate(promotion.endsAt)}</td>
                    <td>
                      {payment && (payment.status === "PENDING" || payment.status === "REJECTED") && (
                        <Link to={`/my-payments/${payment.id}`} className="btn btn-primary my-promotions-page__pay">
                          {payment.status === "REJECTED" ? "Resubmit Payment" : "Complete Payment"}
                        </Link>
                      )}
                      {payment && payment.status === "SUBMITTED" && (
                        <Link to={`/my-payments/${payment.id}`} className="btn btn-outline my-promotions-page__pay">
                          View Payment
                        </Link>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
