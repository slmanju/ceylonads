import { Link } from "react-router-dom";
import { FaBook } from "react-icons/fa";
import type { TuitionFeaturedCardResponse } from "../../types/api";
import { resolveMediaUrl } from "../../api/apiClient";
import { formatAdPrice } from "../../utils/formatPrice";
import { PromotedBadge } from "../Badge/Badge";
import "./FeaturedTuitionCard.css";

interface FeaturedTuitionCardProps {
  card: TuitionFeaturedCardResponse;
}

export function FeaturedTuitionCard({ card }: FeaturedTuitionCardProps) {
  const primaryTags = [card.subject, card.level].filter((tag): tag is string => !!tag);
  const isOnline = card.deliveryMode?.value === "ONLINE";
  const locationLabel = card.primaryLocation?.name ?? (isOnline ? "Online" : undefined);
  const secondaryTags = [card.curriculum?.label, locationLabel].filter((tag): tag is string => !!tag);

  return (
    <Link to={`/classes/${card.slug}`} className="featured-tuition-card">
      <div className="featured-tuition-card__image-wrap">
        {card.primaryImageUrl ? (
          <img
            className="featured-tuition-card__image"
            src={resolveMediaUrl(card.primaryImageUrl)}
            alt={card.title}
            loading="lazy"
          />
        ) : (
          <div className="featured-tuition-card__image featured-tuition-card__image--fallback">
            <FaBook aria-hidden="true" />
          </div>
        )}
        <div className="featured-tuition-card__badge">
          <PromotedBadge />
        </div>
      </div>

      <div className="featured-tuition-card__body">
        <p className="featured-tuition-card__title" title={card.title}>
          {card.title}
        </p>

        {primaryTags.length > 0 && (
          <p className="featured-tuition-card__meta" title={primaryTags.join(" • ")}>
            {primaryTags.join(" • ")}
          </p>
        )}
        {secondaryTags.length > 0 && (
          <p className="featured-tuition-card__meta featured-tuition-card__meta--muted" title={secondaryTags.join(" • ")}>
            {secondaryTags.join(" • ")}
          </p>
        )}

        <div className="featured-tuition-card__footer">
          {card.price != null && <p className="featured-tuition-card__price">{formatAdPrice(card.price)}</p>}
          <span className="featured-tuition-card__cta">View Class →</span>
        </div>
      </div>
    </Link>
  );
}
