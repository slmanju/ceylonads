import { FaArrowRight } from "react-icons/fa";
import { Link } from "react-router-dom";
import { PromotedBadge } from "../Badge/Badge";
import "./SearchPromoCard.css";
import "./SearchPromoPlaceholderCard.css";

// Compact empty-state fallback for one unsold search-page promo slot (see SearchPromoCard for the
// real-promotion sibling and ClassesPage.tsx for the slot count/backfill logic). Reuses the same
// decorative illustration as the homepage's FeaturedPlaceholderCard, but crops it with
// object-fit: cover (search-promo-card__image--cover) since this is a compact landscape frame
// rather than a portrait one - safe only because the illustration is decorative and carries no
// text of its own. Copy is intentionally shorter than the homepage placeholder's (no repeated
// "Featured Placement" eyebrow - the PROMOTED badge already says that) to keep the card compact.
export function SearchPromoPlaceholderCard() {
  return (
    <Link to="/post-ad" className="search-promo-card search-promo-card--placeholder">
      <div className="search-promo-card__image-wrap">
        <img
          src="/images/featured-placeholder.png"
          alt=""
          className="search-promo-card__image search-promo-card__image--cover"
          loading="lazy"
        />
        <div className="search-promo-card__badge">
          <PromotedBadge />
        </div>
      </div>

      <div className="search-promo-card__body">
        <p className="search-promo-card__title">Promote your class here</p>
        <p className="search-promo-card__meta">Reach students searching now.</p>
        <div className="search-promo-card__footer">
          <span className="search-promo-card__cta">
            Advertise Here <FaArrowRight aria-hidden="true" />
          </span>
        </div>
      </div>
    </Link>
  );
}
