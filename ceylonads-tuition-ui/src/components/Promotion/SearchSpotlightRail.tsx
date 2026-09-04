import type { TuitionPromotion } from "../../tuition/promotion/model/promotion";
import { VerticalPromotionRail } from "./VerticalPromotionRail";
import { SpotlightPosterTile } from "./SpotlightPosterTile";

interface SearchSpotlightRailProps {
  /** Up to 12 active Search Page Spotlight promotions, backend-ordered (startsAt, then id) - see
   *  ClassSearchResults. */
  promotions: TuitionPromotion[];
  /** While the backend request is still in flight, render nothing rather than a placeholder-filled
   *  rail that would flicker away the moment real promotions arrive. */
  loading?: boolean;
}

// Desktop right rail for Search Page Spotlight: a vertical carousel showing 4 poster-only tiles at
// once out of as many as 12 active promotions - see VerticalPromotionRail for the shared
// windowing/autoplay/placeholder-fill mechanics (also used by Homepage Spotlight's
// HomeSpotlightRail) and SpotlightPosterTile for the shared poster frame.
export function SearchSpotlightRail({ promotions, loading = false }: SearchSpotlightRailProps) {
  return (
    <VerticalPromotionRail
      promotions={promotions}
      loading={loading}
      placeholderKeyPrefix="search-spotlight-placeholder"
      renderCard={(promotion) => <SpotlightPosterTile imageUrl={promotion.imageUrl} target={promotion.target} />}
      renderPlaceholder={() => <SpotlightPosterTile isPlaceholder imageUrl="/images/featured-placeholder.png" />}
    />
  );
}
