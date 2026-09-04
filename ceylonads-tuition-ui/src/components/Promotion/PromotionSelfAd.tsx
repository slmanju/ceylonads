import { Link } from "react-router-dom";
import "./PromotionSelfAd.css";

// UI-owned house ad, honest about being CeylonAds' own promo rather than a real advertiser.
// Used as the empty-state fallback for fixed single-card placements (PromotionSidebar, the Class
// Detail Page's side/mobile promo slots), so a slot with no real promotion never collapses to a
// blank/missing column. Carousel-shaped placements (Homepage Spotlight, Search Page Spotlight) use
// their own poster-only placeholder tiles instead - see SpotlightPosterTile.
export function PromotionSelfAd() {
  return (
    <div className="promotion-self-ad">
      <span className="promotion-self-ad__eyebrow">Advertise Here</span>
      <p className="promotion-self-ad__title">Reach students across Sri Lanka</p>
      <p className="promotion-self-ad__body">Promote your class, tutor profile or institute.</p>
      <Link to="/post-ad" className="btn btn-accent btn-block promotion-self-ad__cta">
        Promote Your Class
      </Link>
    </div>
  );
}
