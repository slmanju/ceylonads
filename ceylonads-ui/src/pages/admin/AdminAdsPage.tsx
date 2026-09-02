import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import * as moderationApi from "../../api/moderationApi";
import * as adsApi from "../../api/adsApi";
import { AdminPageHeader } from "../../components/AdminPageHeader/AdminPageHeader";
import { AdminAdCard } from "../../components/AdminAdCard/AdminAdCard";
import { ConfirmDialog } from "../../components/ConfirmDialog/ConfirmDialog";
import { Pagination } from "../../components/Pagination/Pagination";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { useToast } from "../../components/Toast/ToastProvider";
import { getApiErrorMessage } from "../../utils/apiError";
import type { AdResponse } from "../../types/api";
import "./AdminAdsPage.css";

type Tab = "PENDING" | "ACTIVE";

const PAGE_SIZE = 12;

export function AdminAdsPage() {
  const { showToast } = useToast();
  const location = useLocation();
  // Reused at both /admin/ads (Admin) and /moderation (Moderator + Admin) - see App.tsx.
  const basePath = location.pathname.startsWith("/moderation") ? "/moderation" : "/admin/ads";
  const [tab, setTab] = useState<Tab>("PENDING");

  const [pendingAds, setPendingAds] = useState<AdResponse[]>([]);
  const [activeAds, setActiveAds] = useState<AdResponse[]>([]);
  const [activePage, setActivePage] = useState(0);
  const [activeTotalPages, setActiveTotalPages] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [pendingReject, setPendingReject] = useState<AdResponse | null>(null);
  const [pendingDeactivate, setPendingDeactivate] = useState<AdResponse | null>(null);
  const [busyAdId, setBusyAdId] = useState<number | null>(null);
  const [dialogLoading, setDialogLoading] = useState(false);

  const loadPending = () => {
    setLoading(true);
    setError(null);
    return moderationApi
      .listPendingAds()
      .then((data) => setPendingAds(data))
      .catch((err) => setError(getApiErrorMessage(err, "Could not load pending ads.")))
      .finally(() => setLoading(false));
  };

  const loadActive = (page: number) => {
    setLoading(true);
    setError(null);
    return adsApi
      .searchAds({ page, size: PAGE_SIZE, sort: "newest" })
      .then((data) => {
        setActiveAds(data.content);
        setActiveTotalPages(data.totalPages);
        setActivePage(data.page);
      })
      .catch((err) => setError(getApiErrorMessage(err, "Could not load active ads.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (tab === "PENDING") {
      loadPending();
    } else {
      loadActive(0);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab]);

  const handleApprove = async (ad: AdResponse) => {
    setBusyAdId(ad.id);
    try {
      await moderationApi.approveAd(ad.id);
      setPendingAds((prev) => prev.filter((a) => a.id !== ad.id));
      showToast("Ad approved.");
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not approve this ad."), "error");
    } finally {
      setBusyAdId(null);
    }
  };

  const confirmReject = async () => {
    if (!pendingReject) return;
    setDialogLoading(true);
    try {
      await moderationApi.rejectAd(pendingReject.id);
      setPendingAds((prev) => prev.filter((a) => a.id !== pendingReject.id));
      showToast("Ad rejected.");
      setPendingReject(null);
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not reject this ad."), "error");
    } finally {
      setDialogLoading(false);
    }
  };

  const confirmDeactivate = async () => {
    if (!pendingDeactivate) return;
    setDialogLoading(true);
    try {
      await moderationApi.deactivateAd(pendingDeactivate.id);
      showToast("Ad deactivated.");
      setPendingDeactivate(null);
      loadActive(activePage);
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not deactivate this ad."), "error");
    } finally {
      setDialogLoading(false);
    }
  };

  const ads = tab === "PENDING" ? pendingAds : activeAds;

  return (
    <div className="admin-ads-page">
      <AdminPageHeader title="Ads" subtitle="Moderate submitted ads and manage live listings." />

      <div className="admin-ads-page__tabs">
        <button
          type="button"
          className={`admin-ads-page__tab ${tab === "PENDING" ? "admin-ads-page__tab--active" : ""}`}
          onClick={() => setTab("PENDING")}
        >
          Pending
        </button>
        <button
          type="button"
          className={`admin-ads-page__tab ${tab === "ACTIVE" ? "admin-ads-page__tab--active" : ""}`}
          onClick={() => setTab("ACTIVE")}
        >
          Active
        </button>
      </div>

      {loading && <LoadingState label="Loading ads…" />}

      {!loading && error && (
        <ErrorState message={error} onRetry={() => (tab === "PENDING" ? loadPending() : loadActive(activePage))} />
      )}

      {!loading && !error && ads.length === 0 && (
        <EmptyState
          title={tab === "PENDING" ? "No ads are waiting for review." : "No active ads found."}
        />
      )}

      {!loading && !error && ads.length > 0 && (
        <>
          <div className="admin-ads-page__list">
            {ads.map((ad) =>
              tab === "PENDING" ? (
                <AdminAdCard
                  key={ad.id}
                  ad={ad}
                  busy={busyAdId === ad.id}
                  onApprove={handleApprove}
                  onReject={setPendingReject}
                  basePath={basePath}
                />
              ) : (
                <AdminAdCard key={ad.id} ad={ad} onDeactivate={setPendingDeactivate} basePath={basePath} />
              ),
            )}
          </div>

          {tab === "ACTIVE" && (
            <Pagination page={activePage} totalPages={activeTotalPages} onPageChange={(p) => loadActive(p)} />
          )}
        </>
      )}

      <ConfirmDialog
        open={pendingReject !== null}
        title="Reject this ad?"
        message={`"${pendingReject?.title}" will not be published and the seller will be notified of the rejection.`}
        confirmLabel="Reject Ad"
        danger
        loading={dialogLoading}
        onConfirm={confirmReject}
        onCancel={() => setPendingReject(null)}
      />

      <ConfirmDialog
        open={pendingDeactivate !== null}
        title="Deactivate this ad?"
        message="The ad will no longer appear in public listings."
        confirmLabel="Deactivate"
        danger
        loading={dialogLoading}
        onConfirm={confirmDeactivate}
        onCancel={() => setPendingDeactivate(null)}
      />
    </div>
  );
}
