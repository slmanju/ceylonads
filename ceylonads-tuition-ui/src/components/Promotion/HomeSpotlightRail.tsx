import type { TuitionPromotion } from "../../tuition/promotion/model/promotion";
import { VerticalPromotionRail } from "./VerticalPromotionRail";
import { SpotlightPosterTile } from "./SpotlightPosterTile";

interface HomeSpotlightRailProps {
  /** Active Homepage Spotlight promotions (TUITION_HOME_LATEST_RIGHT), backend-ordered (startsAt,
   *  then id) - see HomePage.tsx. */
  promotions: TuitionPromotion[];
  /** While the backend request is still in flight, render nothing rather than a placeholder-filled
   *  rail that would flicker away the moment real promotions arrive. */
  loading?: boolean;
}

// Homepage's right-rail vertical carousel beside Latest Classes: a real, independently-purchasable
// slot (TUITION_HOME_LATEST_RIGHT, "Homepage Spotlight"), distinct from the Featured Classes
// horizontal carousel above it (TUITION_FEATURED) - two separate paid products. Tiles here are
// poster-only (see SpotlightPosterTile, shared with Search Page Spotlight's SearchSpotlightRail) -
// no title, price, or metadata. Showing up to 4 tiles at once - see VerticalPromotionRail for the
// shared windowing/autoplay/placeholder-fill mechanics.
export function HomeSpotlightRail({ promotions, loading = false }: HomeSpotlightRailProps) {
  return (
    <VerticalPromotionRail
      promotions={promotions}
      loading={loading}
      placeholderKeyPrefix="home-spotlight-placeholder"
      renderCard={(promotion) => <SpotlightPosterTile imageUrl={promotion.imageUrl} target={promotion.target} />}
      renderPlaceholder={() => <SpotlightPosterTile isPlaceholder imageUrl="/images/featured-placeholder.png" />}
    />
  );
}
