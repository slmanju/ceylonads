import type { TuitionFeaturedCardResponse } from "../../types/api";
import { featuredCardToPromotion } from "../../tuition/promotion/api/tuitionPromotionApi";
import { FeaturedTuitionCarousel } from "../FeaturedTuitionCarousel/FeaturedTuitionCarousel";
import { SpotlightPosterTile } from "./SpotlightPosterTile";

interface SearchSpotlightInlineProps {
  /** Same up-to-12 Search Page Spotlight promotions SearchSpotlightRail renders in the desktop
   *  rail (see ClassSearchResults) - raw cards here since FeaturedTuitionCarousel's renderItem
   *  hook is the one that adapts each into the shared SpotlightPosterTile presentation. */
  promotions: TuitionFeaturedCardResponse[];
  /** While the backend request is still in flight, defer to FeaturedTuitionCarousel's own loading
   *  state rather than rendering a placeholder-filled carousel that would flicker away. */
  loading?: boolean;
}

// Mobile/tablet inline presentation of Search Page Spotlight (<1080px - see
// ClassSearchResults.css), shown among the organic cards instead of the desktop right rail, which
// this breakpoint has no room for. Reuses FeaturedTuitionCarousel's existing horizontal carousel
// (circular one-at-a-time navigation, autoplay, loop, hover/focus/touch pause,
// prefers-reduced-motion, edge arrows, and minimum-item placeholder fill) rather than
// re-implementing that behavior for a second orientation. Renders the same poster-only
// SpotlightPosterTile as the desktop rail, not a text-heavy card.
export function SearchSpotlightInline({ promotions, loading = false }: SearchSpotlightInlineProps) {
  return (
    <FeaturedTuitionCarousel
      items={promotions}
      loading={loading}
      placeholderKeyPrefix="search-spotlight-inline-placeholder"
      compact
      renderItem={(card) => {
        const promotion = featuredCardToPromotion(card, "TUITION_SEARCH_SIDEBAR_TOP", "PROMOTED");
        return <SpotlightPosterTile imageUrl={promotion.imageUrl} target={promotion.target} />;
      }}
      renderPlaceholder={() => <SpotlightPosterTile isPlaceholder imageUrl="/images/featured-placeholder.png" />}
    />
  );
}
