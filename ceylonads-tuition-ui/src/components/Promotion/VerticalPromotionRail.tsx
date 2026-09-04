import type { ReactNode } from "react";
import { FaChevronDown, FaChevronUp } from "react-icons/fa";
import type { TuitionPromotion } from "../../tuition/promotion/model/promotion";
import { ensureCarouselMinimumItems } from "../../utils/ensureCarouselMinimumItems";
import { useCircularCarousel } from "../../hooks/useCircularCarousel";
import "./VerticalPromotionRail.css";

interface VerticalPromotionRailProps {
  /** Real active promotions for this placement, backend-ordered (startsAt, then id). */
  promotions: TuitionPromotion[];
  /** While the backend request is still in flight, render nothing rather than a placeholder-filled
   *  rail that would flicker away the moment real promotions arrive. */
  loading?: boolean;
  /** Cards visible per page. Defaults to 4 (the desktop visible count shared by every vertical
   *  promotion rail today). */
  visibleCount?: number;
  ariaLabel?: string;
  eyebrow?: string;
  /** Placement-scoped prefix for placeholder keys (e.g. "home-spotlight-placeholder") - see
   *  ensureCarouselMinimumItems. Must be stable and unique per rail instance on the page. */
  placeholderKeyPrefix: string;
  renderCard: (promotion: TuitionPromotion) => ReactNode;
  renderPlaceholder: () => ReactNode;
}

// Shared vertical carousel - a right-rail (or, once its grid column collapses at narrow widths, a
// full-width stacked) presentation showing up to `visibleCount` compact cards at once. Backs both
// Search Page Spotlight (SearchSpotlightRail) and Homepage Spotlight (HomeSpotlightRail). Every
// promotional carousel is composed with at least visibleCount+1 items (real promotions first,
// "Advertise Here" placeholders filling the rest - see ensureCarouselMinimumItems), so up/down
// arrows always have somewhere new to go: each press shifts the window by exactly one card and
// wraps circularly (see useCircularCarousel) - never a dead end.
export function VerticalPromotionRail({
  promotions,
  loading = false,
  visibleCount = 4,
  ariaLabel = "Sponsored",
  eyebrow = "Promotions",
  placeholderKeyPrefix,
  renderCard,
  renderPlaceholder,
}: VerticalPromotionRailProps) {
  const composed = ensureCarouselMinimumItems(promotions, visibleCount, placeholderKeyPrefix);
  const { visibleIndices, next, prev, canLoop, containerProps } = useCircularCarousel({
    itemCount: composed.length,
    visibleCount,
  });

  // While genuinely loading, render nothing rather than a placeholder-filled rail that would
  // flicker away the moment real promotions arrive (see root CLAUDE.md's promotion placeholder
  // spec on async loading).
  if (loading) {
    return null;
  }

  const showControls = canLoop;

  return (
    <aside className="vertical-promotion-rail" aria-label={ariaLabel} {...containerProps}>
      <span className="vertical-promotion-rail__eyebrow">{eyebrow}</span>

      {showControls && (
        <button type="button" className="vertical-promotion-rail__chevron" aria-label="Previous promotions" onClick={prev}>
          <FaChevronUp aria-hidden="true" />
        </button>
      )}

      <div className="vertical-promotion-rail__track">
        {visibleIndices.map((index) => {
          const slot = composed[index];
          return <div key={slot.kind === "real" ? slot.item.id : slot.key}>{slot.kind === "real" ? renderCard(slot.item) : renderPlaceholder()}</div>;
        })}
      </div>

      {showControls && (
        <button type="button" className="vertical-promotion-rail__chevron" aria-label="Next promotions" onClick={next}>
          <FaChevronDown aria-hidden="true" />
        </button>
      )}
    </aside>
  );
}
