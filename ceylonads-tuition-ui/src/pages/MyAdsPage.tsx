import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { getMyAds, deactivateAd } from "../api/adsApi";
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
      setAllAds((prev) => prev.map((ad) => (ad.id === pendingDeactivate.id ? { ...ad, status: "DEACTIVATED" } : ad)));
      setPendingDeactivate(null);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not deactivate this ad."));
    } finally {
      setDeactivating(false);
    }
  };

  return (
    <div className="my-ads-page container">
      <Seo title="My Classes" noindex />
      <div className="my-ads-page__header">
        <h1>My Classes</h1>
        <Link to="/post-ad" className="btn btn-primary">
          Post Tuition Ad
        </Link>
      </div>

      {flash && !dismissedFlash && (
        <div className="my-ads-page__flash" role="status">
          <span>
            {flash === "created" ? "Your ad has been submitted for review." : "Your ad has been updated and is now pending review."}
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
          title="You haven't posted any tuition ads yet."
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
                {status.replace("_", " ")} ({ads.filter((a) => a.status === status).length})
              </button>
            ))}
          </div>

          {visibleAds.length === 0 ? (
            <EmptyState title="No ads in this status." />
          ) : (
            <div className="my-ads-page__grid">
              {visibleAds.map((ad) => (
                <MyClassCard
                  key={ad.id}
                  ad={ad}
                  onDeactivate={setPendingDeactivate}
                  promotions={promotions.filter((p) => p.adId === ad.id)}
                />
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
