import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { FaArrowLeft, FaMapMarkerAlt, FaPhone, FaTag, FaUserCircle } from "react-icons/fa";
import { approveTuitionAd, getTuitionAd, rejectTuitionAd } from "../../api/adminTuitionApi";
import { ImageGallery } from "../../components/ImageGallery/ImageGallery";
import { ConfirmDialog } from "../../components/ConfirmDialog/ConfirmDialog";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { formatAdPrice } from "../../utils/formatPrice";
import { formatAdLocations } from "../../utils/formatLocations";
import { formatRelativeDate } from "../../utils/formatDate";
import { getApiErrorMessage } from "../../utils/apiError";
import type { AdResponse } from "../../types/api";
import "./AdminClassReviewPage.css";

export function AdminClassReviewPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

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
    getTuitionAd(id)
      .then(setAd)
      .catch((err) => setError(getApiErrorMessage(err, "This class could not be found.")))
      .finally(() => setLoading(false));
  };

  useEffect(load, [id]);

  const handleApprove = async () => {
    if (!ad) return;
    setActionLoading(true);
    setActionError(null);
    try {
      await approveTuitionAd(ad.id);
      navigate("/admin/tuition/pending");
    } catch (err) {
      setActionError(getApiErrorMessage(err, "Could not approve this class."));
    } finally {
      setActionLoading(false);
    }
  };

  const confirmReject = async () => {
    if (!ad) return;
    setActionLoading(true);
    setActionError(null);
    try {
      await rejectTuitionAd(ad.id);
      navigate("/admin/tuition/pending");
    } catch (err) {
      setActionError(getApiErrorMessage(err, "Could not reject this class."));
      setShowReject(false);
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) return <LoadingState label="Loading class…" />;
  if (error || !ad) return <ErrorState title="Class not found" message={error ?? "This class is unavailable."} />;

  return (
    <div className="tuition-admin-review">
      <Link to="/admin/tuition/pending" className="tuition-admin-review__back">
        <FaArrowLeft aria-hidden="true" /> Back to Pending Classes
      </Link>

      {actionError && (
        <p className="tuition-admin-review__error" role="alert">
          {actionError}
        </p>
      )}

      <div className="tuition-admin-review__layout">
        <div className="tuition-admin-review__gallery">
          <ImageGallery media={ad.media} title={ad.title} />
        </div>

        <div className="tuition-admin-review__info">
          <span className={`tuition-admin-review__status tuition-admin-review__status--${ad.status.toLowerCase()}`}>
            {ad.status.replace("_", " ")}
          </span>

          <p className="tuition-admin-review__price">{formatAdPrice(ad.price)}</p>
          <h1 className="tuition-admin-review__title">{ad.title}</h1>

          <div className="tuition-admin-review__meta">
            <span>
              <FaTag aria-hidden="true" /> {ad.category}
            </span>
            {formatAdLocations(ad.locations) && (
              <span>
                <FaMapMarkerAlt aria-hidden="true" /> {formatAdLocations(ad.locations)}
              </span>
            )}
            <span>Submitted {formatRelativeDate(ad.createdAt)}</span>
          </div>

          {ad.attributes.length > 0 && (
            <div className="tuition-admin-review__details">
              <h2>Class Details</h2>
              <dl>
                {ad.attributes.map((attr) => (
                  <div key={attr.key} className="tuition-admin-review__details-row">
                    <dt>{attr.name}</dt>
                    <dd>{attr.displayValue}</dd>
                  </div>
                ))}
              </dl>
            </div>
          )}

          <div className="tuition-admin-review__contact">
            <FaUserCircle aria-hidden="true" className="tuition-admin-review__contact-icon" />
            <div>
              <p className="tuition-admin-review__contact-label">Tutor / Seller</p>
              <p className="tuition-admin-review__contact-name">{ad.seller.displayName}</p>
              {(ad.contact?.phoneNumber ?? ad.seller.phone) && (
                <p className="tuition-admin-review__contact-phone">
                  <FaPhone aria-hidden="true" /> {ad.contact?.phoneNumber ?? ad.seller.phone}
                </p>
              )}
            </div>
          </div>

          <div className="tuition-admin-review__description">
            <h2>Description</h2>
            <p>{ad.description}</p>
          </div>

          {ad.status === "PENDING_REVIEW" && (
            <div className="tuition-admin-review__actions">
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
        title="Reject this class?"
        message={`"${ad.title}" will not be published. The tutor will still see it (as Rejected) in My Classes.`}
        confirmLabel="Reject Class"
        danger
        loading={actionLoading}
        onConfirm={confirmReject}
        onCancel={() => setShowReject(false)}
      />
    </div>
  );
}
