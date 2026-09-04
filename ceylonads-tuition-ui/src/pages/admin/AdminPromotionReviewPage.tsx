import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { FaArrowLeft } from "react-icons/fa";
import { approvePromotion, getPromotion, rejectPromotion } from "../../api/adminTuitionPromotionApi";
import { getTuitionAd } from "../../api/adminTuitionApi";
import { ImageGallery } from "../../components/ImageGallery/ImageGallery";
import { ConfirmDialog } from "../../components/ConfirmDialog/ConfirmDialog";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { formatPromotionPrice } from "../../utils/formatPrice";
import { formatFullDate } from "../../utils/formatDate";
import { getApiErrorMessage } from "../../utils/apiError";
import type { AdResponse, PromotionResponse } from "../../types/api";
import "./AdminPromotionReviewPage.css";

export function AdminPromotionReviewPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [promotion, setPromotion] = useState<PromotionResponse | null>(null);
  const [ad, setAd] = useState<AdResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [showReject, setShowReject] = useState(false);

  const load = () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    getPromotion(id)
      .then((p) => {
        setPromotion(p);
        if (p.kind === "AD_PROMOTION" && p.adId) {
          return getTuitionAd(p.adId).then(setAd);
        }
        setAd(null);
      })
      .catch((err) => setError(getApiErrorMessage(err, "This promotion could not be found.")))
      .finally(() => setLoading(false));
  };

  useEffect(load, [id]);

  const handleApprove = async () => {
    if (!promotion) return;
    setActionLoading(true);
    setActionError(null);
    try {
      await approvePromotion(promotion.id);
      navigate("/admin/tuition/promotions");
    } catch (err) {
      setActionError(getApiErrorMessage(err, "Could not approve this promotion."));
    } finally {
      setActionLoading(false);
    }
  };

  const confirmReject = async () => {
    if (!promotion) return;
    setActionLoading(true);
    setActionError(null);
    try {
      await rejectPromotion(promotion.id);
      navigate("/admin/tuition/promotions");
    } catch (err) {
      setActionError(getApiErrorMessage(err, "Could not reject this promotion."));
      setShowReject(false);
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) return <LoadingState label="Loading promotion…" />;
  if (error || !promotion) return <ErrorState title="Promotion not found" message={error ?? "This promotion is unavailable."} />;

  const canReview = promotion.status === "PENDING_PAYMENT" || promotion.status === "PENDING_APPROVAL";

  return (
    <div className="tuition-admin-promo-review">
      <Link to="/admin/tuition/promotions" className="tuition-admin-promo-review__back">
        <FaArrowLeft aria-hidden="true" /> Back to Promotions
      </Link>

      {actionError && (
        <p className="tuition-admin-promo-review__error" role="alert">
          {actionError}
        </p>
      )}

      <div className="tuition-admin-promo-review__layout">
        {ad && (
          <div className="tuition-admin-promo-review__gallery">
            <ImageGallery media={ad.media} title={ad.title} />
          </div>
        )}

        <div className="tuition-admin-promo-review__info">
          <span className={`tuition-admin-promo-review__status tuition-admin-promo-review__status--${promotion.status.toLowerCase()}`}>
            {promotion.status.replace("_", " ")}
          </span>

          <h1>{promotion.adTitle ?? `Banner (${promotion.slotCode})`}</h1>

          <dl className="tuition-admin-promo-review__details">
            <div>
              <dt>Owner</dt>
              <dd>{promotion.customerDisplayName}</dd>
            </div>
            <div>
              <dt>Promotion product</dt>
              <dd>{promotion.promotionPlanName}</dd>
            </div>
            <div>
              <dt>Placement</dt>
              <dd>{promotion.slotCode}</dd>
            </div>
            <div>
              <dt>Duration</dt>
              <dd>{promotion.durationDays} days</dd>
            </div>
            <div>
              <dt>Effective price</dt>
              <dd>{formatPromotionPrice(promotion.price)}</dd>
            </div>
            <div>
              <dt>Requested</dt>
              <dd>{formatFullDate(promotion.createdAt)}</dd>
            </div>
            {ad && (
              <>
                <div>
                  <dt>Class status</dt>
                  <dd>{ad.status.replace("_", " ")}</dd>
                </div>
                <div>
                  <dt>Class expires</dt>
                  <dd>{ad.expiresAt ? formatFullDate(ad.expiresAt) : "—"}</dd>
                </div>
                {ad.contact?.phoneNumber && (
                  <div>
                    <dt>Tutor contact</dt>
                    <dd>{ad.contact.phoneNumber}</dd>
                  </div>
                )}
              </>
            )}
          </dl>

          {ad && (
            <div className="tuition-admin-promo-review__description">
              <h2>Description</h2>
              <p>{ad.description}</p>
            </div>
          )}

          {canReview && (
            <div className="tuition-admin-promo-review__actions">
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
            </div>
          )}
        </div>
      </div>

      <ConfirmDialog
        open={showReject}
        title="Reject this promotion?"
        message="It will not be published and will never render publicly. The record is kept, not deleted."
        confirmLabel="Reject Promotion"
        danger
        loading={actionLoading}
        onConfirm={confirmReject}
        onCancel={() => setShowReject(false)}
      />
    </div>
  );
}
