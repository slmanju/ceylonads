import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getTuitionAdsByStatus } from "../../api/adminTuitionApi";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { formatAdPrice } from "../../utils/formatPrice";
import { formatRelativeDate } from "../../utils/formatDate";
import { getApiErrorMessage } from "../../utils/apiError";
import type { AdResponse, AdStatus } from "../../types/api";
import "./AdminPendingClassesPage.css";

const TABS: { key: AdStatus; label: string }[] = [
  { key: "PENDING_REVIEW", label: "Pending" },
  { key: "ACTIVE", label: "Active" },
  { key: "REJECTED", label: "Rejected" },
  { key: "EXPIRED", label: "Expired" },
];

export function AdminPendingClassesPage() {
  const [activeTab, setActiveTab] = useState<AdStatus>("PENDING_REVIEW");
  const [ads, setAds] = useState<AdResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = (status: AdStatus) => {
    setLoading(true);
    setError(null);
    getTuitionAdsByStatus(status)
      .then(setAds)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load classes.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => load(activeTab), [activeTab]);

  return (
    <div className="tuition-admin-pending">
      <h1>Classes</h1>

      <div className="tuition-admin-pending__tabs">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            className={`tuition-admin-pending__tab ${activeTab === tab.key ? "tuition-admin-pending__tab--active" : ""}`}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {loading && <LoadingState label="Loading classes…" />}
      {!loading && (error || !ads) && <ErrorState message={error ?? undefined} onRetry={() => load(activeTab)} />}

      {!loading && !error && ads && ads.length === 0 && (
        <EmptyState title="Nothing here" message="No classes with this status right now." />
      )}

      {!loading && !error && ads && ads.length > 0 && (
        <ul className="tuition-admin-pending__list">
          {ads.map((ad) => (
            <li key={ad.id}>
              <Link to={`/admin/tuition/pending/${ad.id}`} className="tuition-admin-pending__row">
                <div className="tuition-admin-pending__main">
                  <span className="tuition-admin-pending__title">{ad.title}</span>
                  <span className="tuition-admin-pending__meta">
                    {ad.category} · {formatAdPrice(ad.price)}
                  </span>
                </div>
                <span className="tuition-admin-pending__date">Submitted {formatRelativeDate(ad.createdAt)}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
