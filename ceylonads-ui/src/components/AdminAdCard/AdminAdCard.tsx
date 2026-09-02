import { Link } from "react-router-dom";
import { FaImage, FaMapMarkerAlt, FaUserCircle } from "react-icons/fa";
import type { AdResponse } from "../../types/api";
import { resolveMediaUrl } from "../../api/apiClient";
import { formatAdPrice } from "../../utils/formatPrice";
import { formatAdLocations } from "../../utils/formatLocations";
import { formatRelativeDate } from "../../utils/formatDate";
import { StatusBadge } from "../StatusBadge/StatusBadge";
import "./AdminAdCard.css";

interface AdminAdCardProps {
  ad: AdResponse;
  busy?: boolean;
  onApprove?: (ad: AdResponse) => void;
  onReject?: (ad: AdResponse) => void;
  onDeactivate?: (ad: AdResponse) => void;
  // This card is reused at both /admin/ads (AdminLayout) and /moderation (AppLayout, for
  // MODERATOR + ADMIN) - the "View" link needs to stay under whichever section is rendering it.
  basePath?: string;
}

export function AdminAdCard({ ad, busy, onApprove, onReject, onDeactivate, basePath = "/admin/ads" }: AdminAdCardProps) {
  const image = ad.media[0];
  const locationLabel = formatAdLocations(ad.locations);

  return (
    <div className="admin-ad-card">
      <div className="admin-ad-card__image-wrap">
        {image ? (
          <img className="admin-ad-card__image" src={resolveMediaUrl(image.url)} alt={ad.title} loading="lazy" />
        ) : (
          <div className="admin-ad-card__image admin-ad-card__image--fallback">
            <FaImage aria-hidden="true" />
          </div>
        )}
      </div>

      <div className="admin-ad-card__body">
        <div className="admin-ad-card__top">
          <p className="admin-ad-card__title">{ad.title}</p>
          <StatusBadge status={ad.status} />
        </div>
        <p className="admin-ad-card__price">{formatAdPrice(ad.price)}</p>

        <div className="admin-ad-card__meta">
          <span>{ad.category}</span>
          {locationLabel && (
            <span>
              <FaMapMarkerAlt aria-hidden="true" /> {locationLabel}
            </span>
          )}
          <span>
            <FaUserCircle aria-hidden="true" /> {ad.seller.displayName}
          </span>
          <span>Submitted {formatRelativeDate(ad.createdAt)}</span>
        </div>
      </div>

      <div className="admin-ad-card__actions">
        <Link to={`${basePath}/${ad.id}`} className="btn btn-secondary admin-ad-card__action">
          View
        </Link>
        {onApprove && (
          <button
            type="button"
            className="btn btn-primary admin-ad-card__action"
            disabled={busy}
            onClick={() => onApprove(ad)}
          >
            Approve
          </button>
        )}
        {onReject && (
          <button
            type="button"
            className="btn btn-outline admin-ad-card__action"
            disabled={busy}
            onClick={() => onReject(ad)}
          >
            Reject
          </button>
        )}
        {onDeactivate && (
          <button
            type="button"
            className="btn btn-outline admin-ad-card__action"
            disabled={busy}
            onClick={() => onDeactivate(ad)}
          >
            Deactivate
          </button>
        )}
      </div>
    </div>
  );
}
