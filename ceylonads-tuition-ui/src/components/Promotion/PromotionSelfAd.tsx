import { Link } from "react-router-dom";
import "./PromotionSelfAd.css";

// UI-owned house ad, honest about being CeylonAds' own promo rather than a real advertiser.
// Used as the permanent bottom card in PromotionHomeRail and as the empty-state fallback in
// PromotionSidebar, so a slot with no real promotion never collapses to a blank/missing column.
export function PromotionSelfAd() {
  return (
    <div className="promotion-self-ad">
      <span className="promotion-self-ad__eyebrow">Advertise Here</span>
      <p className="promotion-self-ad__title">Reach students across Sri Lanka</p>
      <p className="promotion-self-ad__body">Promote your class, tutor profile or institute.</p>
      <Link to="/post-ad" className="btn btn-accent btn-block promotion-self-ad__cta">
        Promote Your Ad
      </Link>
    </div>
  );
}
