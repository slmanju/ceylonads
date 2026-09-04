import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listPromotions } from "../../api/adminTuitionPromotionApi";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { formatPromotionPrice } from "../../utils/formatPrice";
import { formatFullDate } from "../../utils/formatDate";
import { getApiErrorMessage } from "../../utils/apiError";
import type { PromotionResponse } from "../../types/api";
import "./AdminPromotionsPage.css";

type Tab = "PENDING" | "ACTIVE" | "REJECTED" | "EXPIRED";

const TABS: { key: Tab; label: string }[] = [
  { key: "PENDING", label: "Pending Review" },
  { key: "ACTIVE", label: "Active" },
  { key: "REJECTED", label: "Rejected" },
  { key: "EXPIRED", label: "Expired" },
];

function matchesTab(promotion: PromotionResponse, tab: Tab): boolean {
  switch (tab) {
    case "PENDING":
      return promotion.status === "PENDING_PAYMENT" || promotion.status === "PENDING_APPROVAL";
    case "ACTIVE":
      return promotion.status === "ACTIVE";
    case "REJECTED":
      return promotion.status === "CANCELLED";
    case "EXPIRED":
      return promotion.status === "EXPIRED";
  }
}

export function AdminPromotionsPage() {
  const [promotions, setPromotions] = useState<PromotionResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<Tab>("PENDING");

  const load = () => {
    setLoading(true);
    setError(null);
    listPromotions()
      .then(setPromotions)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load promotions.")))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  if (loading) return <LoadingState label="Loading promotions…" />;
  if (error || !promotions) return <ErrorState message={error ?? undefined} onRetry={load} />;

  const visible = promotions.filter((p) => matchesTab(p, activeTab));

  return (
    <div className="tuition-admin-promotions">
      <h1>Promotions</h1>

      <div className="tuition-admin-promotions__tabs">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            className={`tuition-admin-promotions__tab ${activeTab === tab.key ? "tuition-admin-promotions__tab--active" : ""}`}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label} ({promotions.filter((p) => matchesTab(p, tab.key)).length})
          </button>
        ))}
      </div>

      {visible.length === 0 ? (
        <EmptyState title="Nothing here" message="No promotions with this status right now." />
      ) : (
        <div className="tuition-admin-promotions__table-wrap">
          <table className="tuition-admin-promotions__table">
            <thead>
              <tr>
                <th>Class</th>
                <th>Owner</th>
                <th>Plan</th>
                <th>Placement</th>
                <th>Price</th>
                <th>Requested</th>
                <th>Starts</th>
                <th>Ends</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visible.map((p) => (
                <tr key={p.id}>
                  <td>{p.adTitle ?? `Banner (${p.slotCode})`}</td>
                  <td>{p.customerDisplayName}</td>
                  <td>{p.promotionPlanName}</td>
                  <td>{p.slotCode}</td>
                  <td>{formatPromotionPrice(p.price)}</td>
                  <td>{formatFullDate(p.createdAt)}</td>
                  <td>{p.startsAt ? formatFullDate(p.startsAt) : "—"}</td>
                  <td>{p.endsAt ? formatFullDate(p.endsAt) : "—"}</td>
                  <td>
                    <Link to={`/admin/tuition/promotions/${p.id}`} className="btn btn-outline tuition-admin-promotions__view-btn">
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
