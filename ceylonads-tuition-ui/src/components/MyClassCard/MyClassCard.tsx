import { Link } from "react-router-dom";
import { FaBook, FaMapMarkerAlt } from "react-icons/fa";
import type { AdResponse, PromotionResponse } from "../../types/api";
import { resolveMediaUrl } from "../../api/apiClient";
import { formatAdPrice } from "../../utils/formatPrice";
import { formatAdLocations } from "../../utils/formatLocations";
import { formatRelativeDate, formatExpiryLabel } from "../../utils/formatDate";
import { StatusBadge } from "../Badge/StatusBadge";
import { PromotedBadge } from "../Badge/Badge";
import "./MyClassCard.css";

interface MyClassCardProps {
  ad: AdResponse;
  onDeactivate: (ad: AdResponse) => void;
  onRenew: (ad: AdResponse) => void;
  renewing?: boolean;
  // This class's own promotions only (any placement/plan), newest first - drives the
  // Promote / Manage Promotion / Promote Again state below. Empty when it has none.
  promotions?: PromotionResponse[];
}

const LIVE_STATUSES = new Set(["PENDING_PAYMENT", "PENDING_APPROVAL", "ACTIVE"]);
const RENEWAL_WINDOW_DAYS = 7;

export function MyClassCard({ ad, onDeactivate, onRenew, renewing = false, promotions = [] }: MyClassCardProps) {
  const image = ad.media[0];
  const canView = ad.status === "ACTIVE";
  const canDeactivate = ad.status !== "DEACTIVATED";
  const canPromote = ad.status === "ACTIVE";
  const locationLabel = formatAdLocations(ad.locations);
  const expiryLabel = formatExpiryLabel(ad.expiresAt);

  // Mirrors the backend's renewal-eligibility rule (see TuitionClassService.renew): EXPIRED, or
  // ACTIVE and within 7 days of expiring. Purely a UX hint - the backend re-checks eligibility
  // itself and is authoritative.
  const daysRemaining = ad.expiresAt ? Math.ceil((new Date(ad.expiresAt).getTime() - Date.now()) / (1000 * 60 * 60 * 24)) : null;
  const canRenew = ad.status === "EXPIRED" || (ad.status === "ACTIVE" && daysRemaining !== null && daysRemaining <= RENEWAL_WINDOW_DAYS);

  const hasLivePromotion = promotions.some((p) => LIVE_STATUSES.has(p.status));
  const hasActivePromotion = promotions.some((p) => p.status === "ACTIVE");
  const promoteLabel = hasLivePromotion ? "Manage Promotion" : promotions.length > 0 ? "Promote Again" : "Promote";

  return (
    <div className="my-class-card">
      <div className="my-class-card__image-wrap">
        {image ? (
          <img className="my-class-card__image" src={resolveMediaUrl(image.url)} alt={ad.title} loading="lazy" />
        ) : (
          <div className="my-class-card__image my-class-card__image--fallback">
            <FaBook aria-hidden="true" />
          </div>
        )}
        <div className="my-class-card__status">
          <StatusBadge status={ad.status} />
          {hasActivePromotion && <PromotedBadge />}
        </div>
      </div>

      <div className="my-class-card__body">
        <p className="my-class-card__price">{formatAdPrice(ad.price)}</p>
        <p className="my-class-card__title">{ad.title}</p>

        <div className="my-class-card__meta">
          {locationLabel && (
            <span>
              <FaMapMarkerAlt aria-hidden="true" />
              {locationLabel}
            </span>
          )}
          <span>{expiryLabel ?? formatRelativeDate(ad.publishedAt ?? ad.createdAt)}</span>
        </div>

        <div className="my-class-card__actions">
          {canView && (
            <Link to={`/classes/${ad.slug}`} className="btn btn-secondary my-class-card__action">
              View
            </Link>
          )}
          <Link to={`/my-ads/${ad.id}/edit`} className="btn btn-secondary my-class-card__action">
            Edit
          </Link>
          {canPromote && (
            <Link to={`/my-ads/${ad.id}/promote`} className="btn btn-accent my-class-card__action">
              {promoteLabel}
            </Link>
          )}
          {canRenew && (
            <button
              type="button"
              className="btn btn-accent my-class-card__action"
              onClick={() => onRenew(ad)}
              disabled={renewing}
            >
              {renewing ? "Renewing…" : "Renew"}
            </button>
          )}
          {canDeactivate && (
            <button type="button" className="btn btn-outline my-class-card__action" onClick={() => onDeactivate(ad)}>
              Deactivate
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
