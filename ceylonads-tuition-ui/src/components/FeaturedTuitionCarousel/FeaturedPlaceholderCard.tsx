import { FaArrowRight } from "react-icons/fa";
import { Link } from "react-router-dom";
import { PromotedBadge } from "../Badge/Badge";
import "./FeaturedTuitionCard.css";
import "./FeaturedPlaceholderCard.css";

// Empty-state fallback for one unsold TUITION_FEATURED slot (see PromotionSlot.visibleCount on
// the backend) - reuses FeaturedTuitionCard's card shell/media frame so a partially-sold Featured
// Classes row stays visually balanced, but with a dashed border and a decorative illustration
// instead of a real poster, so it never reads as a real listing. Still carries the same
// PromotedBadge as FeaturedTuitionCard - the badge marks the SLOT as promotional inventory, not
// whether it currently has a paying advertiser, so it belongs on both states.
export function FeaturedPlaceholderCard() {
  return (
    <Link to="/post-ad" className="featured-tuition-card featured-tuition-card--placeholder">
      <div className="featured-tuition-card__image-wrap">
        <img
          src="/images/featured-placeholder.png"
          alt=""
          className="featured-tuition-card__image"
          loading="lazy"
        />
        <div className="featured-tuition-card__badge">
          <PromotedBadge />
        </div>
      </div>

      <div className="featured-tuition-card__body">
        <span className="featured-placeholder-card__eyebrow">Featured Placement</span>
        <p className="featured-placeholder-card__title">Promote your class here</p>
        <p className="featured-placeholder-card__body-text">Reach students searching for tuition.</p>

        <div className="featured-tuition-card__footer">
          <span className="featured-tuition-card__cta">
            Advertise Here <FaArrowRight aria-hidden="true" />
          </span>
        </div>
      </div>
    </Link>
  );
}
