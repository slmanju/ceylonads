import { Link } from "react-router-dom";
import { FaMapMarkerAlt, FaImage } from "react-icons/fa";
import type { AdResponse } from "../../types/api";
import { resolveMediaUrl } from "../../api/apiClient";
import { formatAdPrice } from "../../utils/formatPrice";
import { formatAdLocations } from "../../utils/formatLocations";
import { formatRelativeDate } from "../../utils/formatDate";
import { PromotedBadge } from "../PromotedBadge/PromotedBadge";
import { useCategories } from "../../hooks/useCategories";
import { categoryAncestors } from "../../utils/categoryHierarchy";
import "./AdCard.css";

interface AdCardProps {
  ad: AdResponse;
}

export function AdCard({ ad }: AdCardProps) {
  const image = ad.media[0];
  const locationLabel = formatAdLocations(ad.locations);

  // Tuition promotional artwork is portrait poster art, not a landscape product photo - every
  // other category keeps today's landscape cover-crop treatment untouched.
  const { categories } = useCategories();
  const category = categories.find((c) => c.slug === ad.categorySlug);
  const isTuition = category ? categoryAncestors(categories, category)[0]?.slug === "education-tuition" : false;
  const imageWrapClassName = `listing-card__image-wrap ${isTuition ? "listing-card__image-wrap--portrait" : ""}`.trim();

  return (
    <div className="listing-card">
      <Link to={`/ads/${ad.slug}`} className="listing-card__link">
        <div className={imageWrapClassName}>
          {image ? (
            <img className="listing-card__image" src={resolveMediaUrl(image.url)} alt={ad.title} loading="lazy" />
          ) : (
            <div className="listing-card__image listing-card__image--fallback">
              <FaImage aria-hidden="true" />
            </div>
          )}
          {ad.promoted && (
            <div className="listing-card__promoted">
              <PromotedBadge />
            </div>
          )}
        </div>

        <div className="listing-card__body">
          <p className="listing-card__price">{formatAdPrice(ad.price)}</p>
          <p className="listing-card__title">{ad.title}</p>
          <div className="listing-card__meta">
            {locationLabel && (
              <span className="listing-card__location">
                <FaMapMarkerAlt aria-hidden="true" />
                {locationLabel}
              </span>
            )}
            <span className="listing-card__date">{formatRelativeDate(ad.publishedAt ?? ad.createdAt)}</span>
          </div>
        </div>
      </Link>
    </div>
  );
}
