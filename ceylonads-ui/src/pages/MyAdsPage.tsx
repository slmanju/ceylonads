import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { getMyAds, deactivateAd } from "../api/adsApi";
import { MyAdCard } from "../components/MyAdCard/MyAdCard";
import { ConfirmDialog } from "../components/ConfirmDialog/ConfirmDialog";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { EmptyState } from "../components/EmptyState/EmptyState";
import type { AdResponse, AdStatus } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";
import "./MyAdsPage.css";

const STATUS_ORDER: AdStatus[] = ["PENDING_REVIEW", "ACTIVE", "REJECTED", "DRAFT", "SOLD", "EXPIRED", "DEACTIVATED"];

const STATUS_LABELS: Record<AdStatus, string> = {
  DRAFT: "Draft",
  PENDING_REVIEW: "Pending",
  ACTIVE: "Active",
  REJECTED: "Rejected",
  SOLD: "Sold",
  EXPIRED: "Expired",
  DEACTIVATED: "Deactivated",
};

export function MyAdsPage() {
  const location = useLocation();
  const flash = (location.state as { flash?: "created" | "updated" } | null)?.flash;

  const [ads, setAds] = useState<AdResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"ALL" | AdStatus>("ALL");
  const [pendingDeactivate, setPendingDeactivate] = useState<AdResponse | null>(null);
  const [deactivating, setDeactivating] = useState(false);
  const [dismissedFlash, setDismissedFlash] = useState(false);

  const loadAds = () => {
    setLoading(true);
    setError(null);
    return getMyAds()
      .then((data) => setAds(data))
      .catch((err) => setError(getApiErrorMessage(err, "Could not load your ads.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadAds();
  }, []);

  const availableStatuses = STATUS_ORDER.filter((status) => ads.some((ad) => ad.status === status));
  const visibleAds = activeTab === "ALL" ? ads : ads.filter((ad) => ad.status === activeTab);

  const confirmDeactivate = async () => {
    if (!pendingDeactivate) return;
    setDeactivating(true);
    try {
      await deactivateAd(pendingDeactivate.id);
      setAds((prev) => prev.map((ad) => (ad.id === pendingDeactivate.id ? { ...ad, status: "DEACTIVATED" } : ad)));
      setPendingDeactivate(null);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not deactivate this ad."));
    } finally {
      setDeactivating(false);
    }
  };

  return (
    <div className="my-ads-page container">
      <div className="my-ads-page__header">
        <h1>My Ads</h1>
        <Link to="/post-ad" className="btn btn-primary">
          Post Free Ad
        </Link>
      </div>

      {flash && !dismissedFlash && (
        <div className="my-ads-page__flash" role="status">
          <span>
            {flash === "created"
              ? "Your ad has been submitted for review."
              : "Your ad has been updated and is now pending review."}
          </span>
          <button type="button" onClick={() => setDismissedFlash(true)} aria-label="Dismiss">
            ×
          </button>
        </div>
      )}

      {loading && <LoadingState label="Loading your ads…" />}

      {!loading && error && <ErrorState message={error} onRetry={loadAds} />}

      {!loading && !error && ads.length === 0 && (
        <EmptyState
          title="You haven't posted any ads yet."
          action={
            <Link to="/post-ad" className="btn btn-primary">
              Post Your First Ad
            </Link>
          }
        />
      )}

      {!loading && !error && ads.length > 0 && (
        <>
          <div className="my-ads-page__tabs">
            <button
              type="button"
              className={`my-ads-page__tab ${activeTab === "ALL" ? "my-ads-page__tab--active" : ""}`}
              onClick={() => setActiveTab("ALL")}
            >
              All ({ads.length})
            </button>
            {availableStatuses.map((status) => (
              <button
                key={status}
                type="button"
                className={`my-ads-page__tab ${activeTab === status ? "my-ads-page__tab--active" : ""}`}
                onClick={() => setActiveTab(status)}
              >
                {STATUS_LABELS[status]} ({ads.filter((a) => a.status === status).length})
              </button>
            ))}
          </div>

          {visibleAds.length === 0 ? (
            <EmptyState title="No ads in this status." />
          ) : (
            <div className="my-ads-page__grid">
              {visibleAds.map((ad) => (
                <MyAdCard key={ad.id} ad={ad} onDeactivate={setPendingDeactivate} />
              ))}
            </div>
          )}
        </>
      )}

      <ConfirmDialog
        open={pendingDeactivate !== null}
        title="Deactivate this ad?"
        message="It will no longer appear in public listings. You can post a new ad anytime."
        confirmLabel="Deactivate"
        danger
        loading={deactivating}
        onConfirm={confirmDeactivate}
        onCancel={() => setPendingDeactivate(null)}
      />
    </div>
  );
}
