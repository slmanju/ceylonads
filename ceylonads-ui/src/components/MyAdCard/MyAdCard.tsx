import { Link } from "react-router-dom";
import { FaMapMarkerAlt, FaImage } from "react-icons/fa";
import type { AdResponse } from "../../types/api";
import { resolveMediaUrl } from "../../api/apiClient";
import { formatAdPrice } from "../../utils/formatPrice";
import { formatAdLocations } from "../../utils/formatLocations";
import { formatRelativeDate } from "../../utils/formatDate";
import { StatusBadge } from "../StatusBadge/StatusBadge";
import "./MyAdCard.css";

interface MyAdCardProps {
  ad: AdResponse;
  onDeactivate: (ad: AdResponse) => void;
}

export function MyAdCard({ ad, onDeactivate }: MyAdCardProps) {
  const image = ad.media[0];
  const canView = ad.status === "ACTIVE";
  const canDeactivate = ad.status !== "DEACTIVATED";
  const locationLabel = formatAdLocations(ad.locations);

  return (
    <div className="my-ad-card">
      <div className="my-ad-card__image-wrap">
        {image ? (
          <img className="my-ad-card__image" src={resolveMediaUrl(image.url)} alt={ad.title} loading="lazy" />
        ) : (
          <div className="my-ad-card__image my-ad-card__image--fallback">
            <FaImage aria-hidden="true" />
          </div>
        )}
        <div className="my-ad-card__status">
          <StatusBadge status={ad.status} />
        </div>
      </div>

      <div className="my-ad-card__body">
        <p className="my-ad-card__price">{formatAdPrice(ad.price)}</p>
        <p className="my-ad-card__title">{ad.title}</p>

        <div className="my-ad-card__meta">
          {locationLabel && (
            <span className="my-ad-card__location">
              <FaMapMarkerAlt aria-hidden="true" />
              {locationLabel}
            </span>
          )}
          <span>{formatRelativeDate(ad.publishedAt ?? ad.createdAt)}</span>
        </div>

        <div className="my-ad-card__actions">
          {canView && (
            <Link to={`/ads/${ad.id}`} className="btn btn-secondary my-ad-card__action">
              View
            </Link>
          )}
          <Link to={`/my-ads/${ad.id}/edit`} className="btn btn-secondary my-ad-card__action">
            Edit
          </Link>
          {ad.status === "ACTIVE" && (
            <Link to={`/my-ads/${ad.id}/promote`} className="btn btn-primary my-ad-card__action">
              Promote
            </Link>
          )}
          {canDeactivate && (
            <button type="button" className="btn btn-outline my-ad-card__action" onClick={() => onDeactivate(ad)}>
              Deactivate
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
