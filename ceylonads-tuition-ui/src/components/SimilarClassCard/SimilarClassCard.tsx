import { Link } from "react-router-dom";
import { FaBook, FaMapMarkerAlt } from "react-icons/fa";
import type { TuitionClassCardResponse } from "../../types/api";
import { resolveMediaUrl } from "../../api/apiClient";
import { formatAdPrice } from "../../utils/formatPrice";
import "./SimilarClassCard.css";

interface SimilarClassCardProps {
  card: TuitionClassCardResponse;
}

// Renders the lean GET /api/tuition/classes/{slug}/similar card shape - deliberately does not
// reuse ClassCard, which is built around the heavier AdResponse+TuitionDetails pair used by the
// list/grid pages elsewhere in the app.
export function SimilarClassCard({ card }: SimilarClassCardProps) {
  const tags = [card.level, card.subject, card.curriculum?.label].filter((tag): tag is string => !!tag);

  return (
    <Link to={`/classes/${card.slug}`} className="similar-class-card">
      <div className="similar-class-card__image-wrap">
        {card.primaryImageUrl ? (
          <img
            className="similar-class-card__image"
            src={resolveMediaUrl(card.primaryImageUrl)}
            alt={card.title}
            loading="lazy"
          />
        ) : (
          <div className="similar-class-card__image similar-class-card__image--fallback">
            <FaBook aria-hidden="true" />
          </div>
        )}
      </div>

      <div className="similar-class-card__body">
        <p className="similar-class-card__title" title={card.title}>
          {card.title}
        </p>

        {tags.length > 0 && (
          <div className="similar-class-card__tags">
            {tags.map((tag) => (
              <span key={tag} className="similar-class-card__tag">
                {tag}
              </span>
            ))}
          </div>
        )}

        {card.primaryLocation && (
          <span className="similar-class-card__location" title={card.primaryLocation.name}>
            <FaMapMarkerAlt aria-hidden="true" />
            <span className="similar-class-card__location-text">{card.primaryLocation.name}</span>
          </span>
        )}

        <p className="similar-class-card__price">{formatAdPrice(card.price)}</p>
      </div>
    </Link>
  );
}
