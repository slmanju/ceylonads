import { Link } from "react-router-dom";
import { FaBook } from "react-icons/fa";
import type { TuitionFeaturedCardResponse } from "../../types/api";
import { resolveMediaUrl } from "../../api/apiClient";
import { formatAdPrice } from "../../utils/formatPrice";
import { PromotedBadge } from "../Badge/Badge";
import "./SearchPromoCard.css";

interface SearchPromoCardProps {
  card: TuitionFeaturedCardResponse;
}

// Compact, page-level promotional card for the Tuition search page's fixed top carousel (see
// ClassesPage.tsx) - a short landscape card (~220-260px tall) rather than the homepage's tall
// portrait FeaturedTuitionCard (4:5, ~380px+). Reads the same featured-slot data
// (TuitionFeaturedCardResponse / GET /api/tuition/featured) as the homepage carousel, just
// presented compactly: this is page advertising space to glance past on the way to search
// results, not a discovery card.
export function SearchPromoCard({ card }: SearchPromoCardProps) {
  const isOnline = card.deliveryMode?.value === "ONLINE";
  const locationLabel = card.primaryLocation?.name ?? (isOnline ? "Online" : undefined);
  const metaLine = [card.subject, card.level, locationLabel].filter((v): v is string => !!v).join(" • ");

  return (
    <Link to={`/classes/${card.slug}`} className="search-promo-card">
      <div className="search-promo-card__image-wrap">
        {card.primaryImageUrl ? (
          <img
            className="search-promo-card__image"
            src={resolveMediaUrl(card.primaryImageUrl)}
            alt={card.title}
            loading="lazy"
          />
        ) : (
          <div className="search-promo-card__image search-promo-card__image--fallback">
            <FaBook aria-hidden="true" />
          </div>
        )}
        <div className="search-promo-card__badge">
          <PromotedBadge />
        </div>
      </div>

      <div className="search-promo-card__body">
        <p className="search-promo-card__title" title={card.title}>
          {card.title}
        </p>
        {metaLine && (
          <p className="search-promo-card__meta" title={metaLine}>
            {metaLine}
          </p>
        )}
        <div className="search-promo-card__footer">
          {card.price != null && <span className="search-promo-card__price">{formatAdPrice(card.price)}</span>}
          <span className="search-promo-card__cta">View Class →</span>
        </div>
      </div>
    </Link>
  );
}
