import { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { FaArrowLeft, FaMapMarkerAlt, FaPhone, FaRegClock, FaTag, FaUserCircle } from "react-icons/fa";
import { isAxiosError } from "axios";
import { getAd } from "../../api/adsApi";
import * as moderationApi from "../../api/moderationApi";
import { ImageGallery } from "../../components/ImageGallery/ImageGallery";
import { StatusBadge } from "../../components/StatusBadge/StatusBadge";
import { ConfirmDialog } from "../../components/ConfirmDialog/ConfirmDialog";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { useToast } from "../../components/Toast/ToastProvider";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatAdPrice } from "../../utils/formatPrice";
import { formatAdLocations } from "../../utils/formatLocations";
import { formatRelativeDate } from "../../utils/formatDate";
import type { AdResponse } from "../../types/api";
import "./AdminAdReviewPage.css";

async function loadAdForReview(id: string): Promise<AdResponse> {
  try {
    return await getAd(id);
  } catch (err) {
    if (isAxiosError(err) && err.response?.status === 404) {
      const pending = await moderationApi.listPendingAds();
      const found = pending.find((a) => String(a.id) === String(id));
      if (found) return found;
    }
    throw err;
  }
}

export function AdminAdReviewPage() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  // Reused at both /admin/ads/:id (Admin) and /moderation/:id (Moderator + Admin) - see App.tsx.
  const listPath = location.pathname.startsWith("/moderation") ? "/moderation" : "/admin/ads";
  const { showToast } = useToast();

  const [ad, setAd] = useState<AdResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [showReject, setShowReject] = useState(false);
  const [showDeactivate, setShowDeactivate] = useState(false);

  const load = () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    loadAdForReview(id)
      .then(setAd)
      .catch((err) => setError(getApiErrorMessage(err, "This ad could not be found.")))
      .finally(() => setLoading(false));
  };

  useEffect(load, [id]);

  const handleApprove = async () => {
    if (!ad) return;
    setActionLoading(true);
    try {
      const updated = await moderationApi.approveAd(ad.id);
      setAd(updated);
      showToast("Ad approved.");
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not approve this ad."), "error");
    } finally {
      setActionLoading(false);
    }
  };

  const confirmReject = async () => {
    if (!ad) return;
    setActionLoading(true);
    try {
      const updated = await moderationApi.rejectAd(ad.id);
      setAd(updated);
      showToast("Ad rejected.");
      setShowReject(false);
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not reject this ad."), "error");
    } finally {
      setActionLoading(false);
    }
  };

  const confirmDeactivate = async () => {
    if (!ad) return;
    setActionLoading(true);
    try {
      const updated = await moderationApi.deactivateAd(ad.id);
      setAd(updated);
      showToast("Ad deactivated.");
      setShowDeactivate(false);
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not deactivate this ad."), "error");
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) return <LoadingState label="Loading ad…" />;
  if (error || !ad) return <ErrorState title="Ad not found" message={error ?? "This ad is unavailable."} />;

  return (
    <div className="admin-ad-review">
      <Link to={listPath} className="admin-ad-review__back">
        <FaArrowLeft aria-hidden="true" /> Back to Ads
      </Link>

      <div className="admin-ad-review__layout">
        <div className="admin-ad-review__gallery">
          <ImageGallery media={ad.media} title={ad.title} />
        </div>

        <div className="admin-ad-review__info">
          <div className="admin-ad-review__top">
            <StatusBadge status={ad.status} />
          </div>

          <p className="admin-ad-review__price">{formatAdPrice(ad.price)}</p>
          <h1 className="admin-ad-review__title">{ad.title}</h1>

          <div className="admin-ad-review__meta">
            <span>
              <FaTag aria-hidden="true" /> {ad.category}
            </span>
            {formatAdLocations(ad.locations) && (
              <span>
                <FaMapMarkerAlt aria-hidden="true" /> {formatAdLocations(ad.locations)}
              </span>
            )}
            <span>
              <FaRegClock aria-hidden="true" /> Submitted {formatRelativeDate(ad.createdAt)}
            </span>
          </div>

          <div className="admin-ad-review__seller">
            <FaUserCircle aria-hidden="true" className="admin-ad-review__seller-icon" />
            <div>
              <p className="admin-ad-review__seller-label">Seller</p>
              <p className="admin-ad-review__seller-name">{ad.seller.displayName}</p>
              {ad.seller.phone && (
                <p className="admin-ad-review__seller-phone">
                  <FaPhone aria-hidden="true" /> {ad.seller.phone}
                </p>
              )}
            </div>
          </div>

          <div className="admin-ad-review__description">
            <h2>Description</h2>
            <p>{ad.description}</p>
          </div>

          <div className="admin-ad-review__actions">
            {ad.status === "PENDING_REVIEW" && (
              <>
                <button type="button" className="btn btn-primary" disabled={actionLoading} onClick={handleApprove}>
                  Approve
                </button>
                <button
                  type="button"
                  className="btn btn-outline"
                  disabled={actionLoading}
                  onClick={() => setShowReject(true)}
                >
                  Reject
                </button>
              </>
            )}
            {ad.status === "ACTIVE" && (
              <button
                type="button"
                className="btn btn-outline"
                disabled={actionLoading}
                onClick={() => setShowDeactivate(true)}
              >
                Deactivate
              </button>
            )}
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={showReject}
        title="Reject this ad?"
        message={`"${ad.title}" will not be published and the seller will be notified of the rejection.`}
        confirmLabel="Reject Ad"
        danger
        loading={actionLoading}
        onConfirm={confirmReject}
        onCancel={() => setShowReject(false)}
      />

      <ConfirmDialog
        open={showDeactivate}
        title="Deactivate this ad?"
        message="The ad will no longer appear in public listings."
        confirmLabel="Deactivate"
        danger
        loading={actionLoading}
        onConfirm={confirmDeactivate}
        onCancel={() => setShowDeactivate(false)}
      />
    </div>
  );
}
