import { FaArrowRight, FaBullhorn } from "react-icons/fa";
import { Link } from "react-router-dom";
import "./PromotionBanner.css";
import "./PromotionBannerSelfAd.css";

interface PromotionBannerSelfAdProps {
  /** "large" for the homepage top banner, "compact" for the search-page top banner. */
  size?: "large" | "compact";
}

// Horizontal counterpart to PromotionSelfAd - the empty-state fallback for a full-width
// PromotionBanner slot (e.g. TUITION_HOME_TOP_BANNER) so that row never collapses to a blank
// gap when the slot has no active promotion. Visually distinguished from a real PromotionBanner
// (dashed border, "Advertisement" eyebrow instead of a SPONSORED/FEATURED badge) so it never
// reads as an actual advertiser.
export function PromotionBannerSelfAd({ size = "large" }: PromotionBannerSelfAdProps) {
  return (
    <Link to="/post-ad" className={`promotion-banner promotion-banner--self promotion-banner--${size}`}>
      <span className="promotion-banner__icon" aria-hidden="true">
        <FaBullhorn />
      </span>
      <div className="promotion-banner__body">
        <span className="promotion-banner-self-ad__eyebrow">Advertisement</span>
        <p className="promotion-banner__title">Promote your tuition class</p>
        <p className="promotion-banner__subtitle">Get featured in front of students searching for classes.</p>
      </div>
      <span className="promotion-banner__cta">
        Advertise Here <FaArrowRight aria-hidden="true" />
      </span>
    </Link>
  );
}
