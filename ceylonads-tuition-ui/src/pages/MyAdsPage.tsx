import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { getMyAds, deactivateTuitionClass, renewTuitionClass } from "../api/adsApi";
import { getMyPromotions } from "../api/promotionApi";
import { MyClassCard } from "../components/MyClassCard/MyClassCard";
import { ConfirmDialog } from "../components/ConfirmDialog/ConfirmDialog";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { EmptyState } from "../components/EmptyState/EmptyState";
import { Seo } from "../components/Seo/Seo";
import { useTuitionCategories } from "../hooks/useTuitionCategories";
import type { AdResponse, AdStatus, PromotionResponse } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";
import "./MyAdsPage.css";

const STATUS_ORDER: AdStatus[] = ["PENDING_REVIEW", "ACTIVE", "REJECTED", "DRAFT", "SOLD", "EXPIRED", "DEACTIVATED"];

// Mirrors the backend's 15-concurrent-listing cap (see TuitionClassService.create /
// COUNTED_TOWARD_LIMIT) - a UX hint only, computed from data already fetched for this page rather
// than a dedicated count endpoint. The backend remains authoritative and re-checks this itself.
const MAX_ACTIVE_LISTINGS = 15;
const COUNTED_TOWARD_LIMIT = new Set<AdStatus>(["PENDING_REVIEW", "ACTIVE"]);

export function MyAdsPage() {
  const location = useLocation();
  const flash = (location.state as { flash?: "created" | "updated" } | null)?.flash;
  const { bySlug: tuitionCategorySlugs } = useTuitionCategories();

  const [allAds, setAllAds] = useState<AdResponse[]>([]);
  const [promotions, setPromotions] = useState<PromotionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"ALL" | AdStatus>("ALL");
  const [pendingDeactivate, setPendingDeactivate] = useState<AdResponse | null>(null);
  const [deactivating, setDeactivating] = useState(false);
  const [dismissedFlash, setDismissedFlash] = useState(false);
  const [renewingId, setRenewingId] = useState<number | null>(null);

  // /api/ads/mine returns every ad the account owns across the whole CeylonAds marketplace
  // (shared account model - see ceylonads-tuition-ui/CLAUDE.md), not just tuition ones. This is a
  // dedicated tuition site, so only ads under the tuition category tree are shown here.
  const ads = allAds.filter((ad) => tuitionCategorySlugs.has(ad.categorySlug));

  const loadAds = () => {
    setLoading(true);
    setError(null);
    return Promise.all([getMyAds(), getMyPromotions()])
      .then(([adsData, promotionsData]) => {
        setAllAds(adsData);
        setPromotions(promotionsData);
      })
      .catch((err) => setError(getApiErrorMessage(err, "Could not load your classes.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadAds();
  }, []);

  const availableStatuses = STATUS_ORDER.filter((status) => ads.some((ad) => ad.status === status));
  const visibleAds = activeTab === "ALL" ? ads : ads.filter((ad) => ad.status === activeTab);

  const activeOrPendingCount = ads.filter((ad) => COUNTED_TOWARD_LIMIT.has(ad.status)).length;
  const atListingLimit = activeOrPendingCount >= MAX_ACTIVE_LISTINGS;

  const confirmDeactivate = async () => {
    if (!pendingDeactivate) return;
    setDeactivating(true);
    try {
      await deactivateTuitionClass(pendingDeactivate.id);
      setAllAds((prev) => prev.map((ad) => (ad.id === pendingDeactivate.id ? { ...ad, status: "DEACTIVATED" } : ad)));
      setPendingDeactivate(null);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not deactivate this class."));
    } finally {
      setDeactivating(false);
    }
  };

  const handleRenew = async (ad: AdResponse) => {
    setRenewingId(ad.id);
    setError(null);
    try {
      await renewTuitionClass(ad.id);
      await loadAds();
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not renew this class."));
    } finally {
      setRenewingId(null);
    }
  };

  return (
    <div className="my-ads-page container">
      <Seo title="My Classes" noindex />
      <div className="my-ads-page__header">
        <h1>My Classes</h1>
        {!loading && !error && ads.length > 0 && (
          <span className="my-ads-page__limit">
            {activeOrPendingCount} of {MAX_ACTIVE_LISTINGS} active/pending classes used
          </span>
        )}
        {atListingLimit ? (
          <button type="button" className="btn btn-primary" disabled title="You've reached the 15-class limit.">
            Post a Class
          </button>
        ) : (
          <Link to="/post-ad" className="btn btn-primary">
            Post a Class
          </Link>
        )}
      </div>

      {flash && !dismissedFlash && (
        <div className="my-ads-page__flash" role="status">
          <span>
            {flash === "created" ? "Your class has been submitted for review." : "Your class has been updated and is now pending review."}
          </span>
          <button type="button" onClick={() => setDismissedFlash(true)} aria-label="Dismiss">
            ×
          </button>
        </div>
      )}

      {loading && <LoadingState label="Loading your classes…" />}

      {!loading && error && <ErrorState message={error} onRetry={loadAds} />}

      {!loading && !error && ads.length === 0 && (
        <EmptyState
          title="You haven't posted any classes yet."
          action={
            <Link to="/post-ad" className="btn btn-primary">
              Post Your First Class
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
                {status.replace("_", " ")} ({ads.filter((a) => a.status === status).length})
              </button>
            ))}
          </div>

          {visibleAds.length === 0 ? (
            <EmptyState title="No classes in this status." />
          ) : (
            <div className="my-ads-page__grid">
              {visibleAds.map((ad) => (
                <MyClassCard
                  key={ad.id}
                  ad={ad}
                  onDeactivate={setPendingDeactivate}
                  onRenew={handleRenew}
                  renewing={renewingId === ad.id}
                  promotions={promotions.filter((p) => p.adId === ad.id)}
                />
              ))}
            </div>
          )}
        </>
      )}

      <ConfirmDialog
        open={pendingDeactivate !== null}
        title="Deactivate this class?"
        message="It will no longer appear in public listings. You can post a new class anytime."
        confirmLabel="Deactivate"
        danger
        loading={deactivating}
        onConfirm={confirmDeactivate}
        onCancel={() => setPendingDeactivate(null)}
      />
    </div>
  );
}
