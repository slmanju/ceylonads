import { useEffect, useState, type FocusEvent } from "react";
import { useMediaQuery } from "./useMediaQuery";

// Shared page-advance interval for every promotional carousel (Homepage/Search/Detail Featured,
// Homepage/Search Spotlight) - centralized here rather than scattered as a literal so every
// promotional carousel stays in lockstep if the pacing ever changes. The Tuition listing image
// gallery (ImageGallery.tsx) is a separate, always-manual component and never reads this constant.
export const PROMOTION_CAROUSEL_AUTOPLAY_MS = 5000;

interface UseCircularCarouselOptions {
  /** Total composed item count (real promotions + placeholders) - every promotional carousel is
   *  composed so this is always > visibleCount once loaded, see ensureCarouselMinimumItems. */
  itemCount: number;
  /** How many items render at once. */
  visibleCount: number;
  /** Defaults to on; has no effect once `canLoop` is false (nothing to advance to), and is
   *  disabled for prefers-reduced-motion. */
  autoplay?: boolean;
  autoplayMs?: number;
}

interface UseCircularCarouselResult {
  /** Indices into the composed item array for the items currently visible, in display order. */
  visibleIndices: number[];
  next: () => void;
  prev: () => void;
  /** Whether there's more than one distinct window to show - i.e. navigation is meaningful.
   *  Every promotional carousel is composed with at least visibleCount+1 items once loaded (see
   *  ensureCarouselMinimumItems), so this is normally always true post-load; it only reads false
   *  before that composition/measurement has happened, or if a caller passes fewer items. */
  canLoop: boolean;
  /** Spread onto the carousel's outer container to wire up autoplay pause-on-hover/focus/touch. */
  containerProps: {
    onMouseEnter: () => void;
    onMouseLeave: () => void;
    onFocus: () => void;
    onBlur: (e: FocusEvent<HTMLElement>) => void;
    onTouchStart: () => void;
    onTouchEnd: () => void;
  };
}

// Shared circular, one-item-at-a-time carousel window used by every promotional carousel
// (FeaturedTuitionCarousel's horizontal row, VerticalPromotionRail's vertical rail). Moving one
// item at a time (rather than a full page of `visibleCount`) keeps the extra item beyond
// visibleCount actually useful - e.g. 4 visible / 5 total advances 1234 -> 2345 -> 3451 -> ...,
// never a near-empty final "page" with only one real card in it. `next`/`prev` wrap circularly
// (last -> first, first -> last) - there is never a dead end.
export function useCircularCarousel({
  itemCount,
  visibleCount,
  autoplay = true,
  autoplayMs = PROMOTION_CAROUSEL_AUTOPLAY_MS,
}: UseCircularCarouselOptions): UseCircularCarouselResult {
  const [startIndex, setStartIndex] = useState(0);

  // Guard against the composed count shrinking (e.g. a real promotion expiring) while sitting on
  // a now out-of-range start index.
  useEffect(() => {
    if (startIndex >= itemCount) setStartIndex(0);
  }, [startIndex, itemCount]);

  const prefersReducedMotion = useMediaQuery("(prefers-reduced-motion: reduce)");
  const [isHovered, setIsHovered] = useState(false);
  const [isFocused, setIsFocused] = useState(false);
  const [isTouching, setIsTouching] = useState(false);
  const [resetKey, setResetKey] = useState(0);

  const canLoop = itemCount > visibleCount;

  function next() {
    if (itemCount === 0) return;
    setStartIndex((i) => (i + 1) % itemCount);
    setResetKey((key) => key + 1);
  }

  function prev() {
    if (itemCount === 0) return;
    setStartIndex((i) => (i - 1 + itemCount) % itemCount);
    setResetKey((key) => key + 1);
  }

  const autoplayActive = autoplay && canLoop && !prefersReducedMotion && !isHovered && !isFocused && !isTouching;

  useEffect(() => {
    if (!autoplayActive) return;
    const id = window.setInterval(() => {
      setStartIndex((i) => (i + 1) % itemCount);
    }, autoplayMs);
    return () => window.clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoplayActive, itemCount, autoplayMs, resetKey]);

  const visibleIndices =
    itemCount === 0 ? [] : Array.from({ length: Math.min(visibleCount, itemCount) }, (_, i) => (startIndex + i) % itemCount);

  return {
    visibleIndices,
    next,
    prev,
    canLoop,
    containerProps: {
      onMouseEnter: () => setIsHovered(true),
      onMouseLeave: () => setIsHovered(false),
      onFocus: () => setIsFocused(true),
      onBlur: (e: FocusEvent<HTMLElement>) => {
        if (!e.relatedTarget || !e.currentTarget.contains(e.relatedTarget as Node)) {
          setIsFocused(false);
        }
      },
      onTouchStart: () => setIsTouching(true),
      onTouchEnd: () => {
        setIsTouching(false);
        setResetKey((key) => key + 1);
      },
    },
  };
}
