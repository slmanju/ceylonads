import { useCallback, useLayoutEffect, useRef, useState, type ReactNode } from "react";
import { FaChevronLeft, FaChevronRight } from "react-icons/fa";
import type { TuitionFeaturedCardResponse } from "../../types/api";
import { ensureCarouselMinimumItems } from "../../utils/ensureCarouselMinimumItems";
import { useCircularCarousel, PROMOTION_CAROUSEL_AUTOPLAY_MS } from "../../hooks/useCircularCarousel";
import { LoadingState } from "../LoadingState/LoadingState";
import { FeaturedTuitionCard } from "./FeaturedTuitionCard";
import { FeaturedPlaceholderCard } from "./FeaturedPlaceholderCard";
import "./FeaturedTuitionCarousel.css";

export { PROMOTION_CAROUSEL_AUTOPLAY_MS };

interface FeaturedTuitionCarouselProps {
  /** Section heading, rendered in a header row alongside the arrows. Omit for a headerless
   *  carousel (see the Tuition search page's fixed promo carousel in ClassesPage.tsx) - the
   *  arrows then render at the track's edges instead of reserving a header row for them. */
  title?: string;
  items: TuitionFeaturedCardResponse[];
  loading: boolean;
  /** Placement-scoped prefix for placeholder keys (e.g. "home-featured-placeholder") - see
   *  ensureCarouselMinimumItems. Must be stable and unique per carousel instance on the page. */
  placeholderKeyPrefix: string;
  /** Tighter heading/arrow/gap chrome for page-level promo carousels like the Tuition search page
   *  (see ClassesPage.tsx) - the homepage leaves this unset and keeps its current sizing. */
  compact?: boolean;
  /** Card renderer - defaults to the homepage's portrait FeaturedTuitionCard. The search page
   *  passes its own compact SearchPromoCard instead; same underlying data, different presentation. */
  renderItem?: (card: TuitionFeaturedCardResponse) => ReactNode;
  /** Placeholder renderer - defaults to FeaturedPlaceholderCard, overridden the same way as renderItem. */
  renderPlaceholder?: () => ReactNode;
  /** Auto-advance by one item every autoPlayInterval ms. Intended for promotional placements only
   *  - defaults to on, since every current caller (Homepage Featured, Search/Detail top
   *  promotions) is promotional. Disabled automatically for prefers-reduced-motion. */
  autoPlay?: boolean;
  /** Autoplay advance interval in ms. Defaults to PROMOTION_CAROUSEL_AUTOPLAY_MS. */
  autoPlayInterval?: number;
}

// Responsive carousel for the homepage "Featured Classes" section (and the search/detail pages'
// compact top promo carousels). Visible-card count is measured from the actual rendered card width
// (via ResizeObserver) rather than duplicating the CSS breakpoints in JS, so the two can never
// drift out of sync. Every promotional carousel is composed with at least visibleCount+1 items
// (real promotions first, "Advertise Here" placeholders filling the rest - see
// ensureCarouselMinimumItems), so arrows always have somewhere new to go: Next/Previous each shift
// the window by exactly one card and wrap circularly (see useCircularCarousel) - never a dead end,
// never a near-empty trailing page.
export function FeaturedTuitionCarousel({
  title,
  items,
  loading,
  placeholderKeyPrefix,
  compact = false,
  renderItem = (card) => <FeaturedTuitionCard card={card} />,
  renderPlaceholder = () => <FeaturedPlaceholderCard />,
  autoPlay = true,
  autoPlayInterval = PROMOTION_CAROUSEL_AUTOPLAY_MS,
}: FeaturedTuitionCarouselProps) {
  const rootClassName = `featured-tuition-carousel${compact ? " featured-tuition-carousel--compact" : ""}`;
  const trackRef = useRef<HTMLDivElement>(null);
  const [visibleCount, setVisibleCount] = useState(1);

  const recomputeVisibleCount = useCallback(() => {
    const track = trackRef.current;
    if (!track) return;
    const firstCard = track.firstElementChild as HTMLElement | null;
    if (!firstCard || firstCard.clientWidth === 0) return;

    const cardWidth = firstCard.getBoundingClientRect().width;
    const gap = parseFloat(getComputedStyle(track).columnGap || "0") || 0;
    const count = Math.max(1, Math.round((track.clientWidth + gap) / (cardWidth + gap)));
    setVisibleCount(count);
  }, []);

  // useLayoutEffect (not useEffect) so the initial measurement lands before the browser paints -
  // otherwise the carousel briefly renders against the default visibleCount of 1 and then jumps to
  // the real breakpoint's count a frame later, flashing extra cards in. Depends on `loading`, not a
  // value derived from visibleCount itself (e.g. composed item count) - that can land on the same
  // value across the loading->loaded transition, which would make React skip re-running this effect
  // for the render where `track` first actually mounts. `loading` flipping false->true is exactly
  // the transition that mounts the track div, so it's the reliable trigger.
  useLayoutEffect(() => {
    const track = trackRef.current;
    if (!track || loading) return;

    recomputeVisibleCount();
    const observer = new ResizeObserver(() => recomputeVisibleCount());
    observer.observe(track);
    return () => observer.disconnect();
  }, [loading, recomputeVisibleCount]);

  const composed = ensureCarouselMinimumItems(items, visibleCount, placeholderKeyPrefix);
  const { visibleIndices, next, prev, canLoop, containerProps } = useCircularCarousel({
    itemCount: composed.length,
    visibleCount,
    autoplay: autoPlay,
    autoplayMs: autoPlayInterval,
  });

  if (loading) {
    return (
      <div className={rootClassName}>
        {title && (
          <div className="featured-tuition-carousel__header">
            <h2>{title}</h2>
          </div>
        )}
        <LoadingState label="Loading featured classes…" />
      </div>
    );
  }

  if (composed.length === 0) {
    return null;
  }

  const showControls = canLoop;
  // Headerless carousels (no title - see the Tuition search page) have nowhere to put the
  // arrows except at the track's own edges, so a header row is never reserved just for them.
  const showEdgeArrows = showControls && !title;

  return (
    <div className={rootClassName} {...containerProps}>
      {title && (
        <div className="featured-tuition-carousel__header">
          <h2>{title}</h2>
          {showControls && (
            <div className="featured-tuition-carousel__arrows">
              <button
                type="button"
                className="featured-tuition-carousel__arrow"
                aria-label="Previous featured classes"
                onClick={prev}
              >
                <FaChevronLeft aria-hidden="true" />
              </button>
              <button
                type="button"
                className="featured-tuition-carousel__arrow"
                aria-label="Next featured classes"
                onClick={next}
              >
                <FaChevronRight aria-hidden="true" />
              </button>
            </div>
          )}
        </div>
      )}

      <div
        className={`featured-tuition-carousel__track-wrap${showEdgeArrows ? " featured-tuition-carousel__track-wrap--edge-arrows" : ""}`}
      >
        {showEdgeArrows && (
          <button
            type="button"
            className="featured-tuition-carousel__edge-arrow featured-tuition-carousel__edge-arrow--prev"
            aria-label="Previous promotions"
            onClick={prev}
          >
            <FaChevronLeft aria-hidden="true" />
          </button>
        )}

        <div className="featured-tuition-carousel__track" ref={trackRef}>
          {visibleIndices.map((index) => {
            const slot = composed[index];
            return (
              <div className="featured-tuition-carousel__item" key={slot.kind === "real" ? slot.item.id : slot.key}>
                {slot.kind === "real" ? renderItem(slot.item) : renderPlaceholder()}
              </div>
            );
          })}
        </div>

        {showEdgeArrows && (
          <button
            type="button"
            className="featured-tuition-carousel__edge-arrow featured-tuition-carousel__edge-arrow--next"
            aria-label="Next promotions"
            onClick={next}
          >
            <FaChevronRight aria-hidden="true" />
          </button>
        )}
      </div>
    </div>
  );
}
