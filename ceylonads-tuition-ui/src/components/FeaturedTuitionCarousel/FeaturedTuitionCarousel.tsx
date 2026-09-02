import { useCallback, useEffect, useRef, useState, type FocusEvent, type ReactNode } from "react";
import { FaChevronLeft, FaChevronRight } from "react-icons/fa";
import type { TuitionFeaturedCardResponse } from "../../types/api";
import { useMediaQuery } from "../../hooks/useMediaQuery";
import { LoadingState } from "../LoadingState/LoadingState";
import { FeaturedTuitionCard } from "./FeaturedTuitionCard";
import { FeaturedPlaceholderCard } from "./FeaturedPlaceholderCard";
import "./FeaturedTuitionCarousel.css";

// Default page-advance interval for promotional placements (Homepage Featured, Search/Detail top
// promotions). Centralized here rather than scattered as a literal so every promotional carousel
// stays in lockstep if the pacing ever changes. The Tuition listing image gallery
// (ImageGallery.tsx) is a separate, always-manual component and never reads this constant.
export const PROMOTION_CAROUSEL_AUTOPLAY_MS = 5000;

interface FeaturedTuitionCarouselProps {
  /** Section heading, rendered in a header row alongside the arrows. Omit for a headerless
   *  carousel (see the Tuition search page's fixed promo carousel in ClassesPage.tsx) - the
   *  arrows then render at the track's edges instead of reserving a header row for them. */
  title?: string;
  items: TuitionFeaturedCardResponse[];
  loading: boolean;
  /** Unsold TUITION_FEATURED slots to backfill with a placeholder card, so a partially (or fully)
   *  unsold row never shrinks or disappears - see HomePage.tsx / ClassesPage.tsx. */
  placeholderCount?: number;
  /** Tighter heading/arrow/gap chrome for page-level promo carousels like the Tuition search page
   *  (see ClassesPage.tsx) - the homepage leaves this unset and keeps its current sizing. */
  compact?: boolean;
  /** Card renderer - defaults to the homepage's portrait FeaturedTuitionCard. The search page
   *  passes its own compact SearchPromoCard instead; same underlying data, different presentation. */
  renderItem?: (card: TuitionFeaturedCardResponse) => ReactNode;
  /** Placeholder renderer - defaults to FeaturedPlaceholderCard, overridden the same way as renderItem. */
  renderPlaceholder?: () => ReactNode;
  /** Auto-advance by one page every autoPlayInterval ms. Intended for promotional placements only
   *  - defaults to on, since every current caller (Homepage Featured, Search/Detail top
   *  promotions) is promotional. Disabled automatically for prefers-reduced-motion. */
  autoPlay?: boolean;
  /** Autoplay page-advance interval in ms. Defaults to PROMOTION_CAROUSEL_AUTOPLAY_MS. */
  autoPlayInterval?: number;
}

// One dot per page reads fine up to a handful of pages, but a 12-slot row on a single-card-wide
// mobile viewport would be 12 pages - past this many, swap to a compact "2 / 12" position readout
// (arrows + swipe still do the actual navigating) rather than rendering a wall of tiny dots.
const MAX_DOTS = 6;

// Responsive carousel for the homepage "Featured Classes" section. Visible-card count is measured
// from the actual rendered card width (via ResizeObserver) rather than duplicating the CSS
// breakpoints in JS, so the two can never drift out of sync. Navigation is native horizontal
// scroll + CSS scroll-snap; the arrows/dots are just programmatic scrollTo calls on top of it, so
// touch/trackpad swipe keeps working for free. Next/Previous always land on a page boundary (a
// full screen-width scroll), never a single card, since scrollToPage always targets
// `page * track.clientWidth`.
export function FeaturedTuitionCarousel({
  title,
  items,
  loading,
  placeholderCount = 0,
  compact = false,
  renderItem = (card) => <FeaturedTuitionCard card={card} />,
  renderPlaceholder = () => <FeaturedPlaceholderCard />,
  autoPlay = true,
  autoPlayInterval = PROMOTION_CAROUSEL_AUTOPLAY_MS,
}: FeaturedTuitionCarouselProps) {
  const rootClassName = `featured-tuition-carousel${compact ? " featured-tuition-carousel--compact" : ""}`;
  const trackRef = useRef<HTMLDivElement>(null);
  const [visibleCount, setVisibleCount] = useState(1);
  const [activePage, setActivePage] = useState(0);
  const totalCount = items.length + placeholderCount;

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

  useEffect(() => {
    const track = trackRef.current;
    if (!track || totalCount === 0) return;

    recomputeVisibleCount();
    const observer = new ResizeObserver(() => recomputeVisibleCount());
    observer.observe(track);
    return () => observer.disconnect();
  }, [totalCount, recomputeVisibleCount]);

  useEffect(() => {
    const track = trackRef.current;
    if (!track) return;

    function onScroll() {
      if (!track || track.clientWidth === 0) return;
      setActivePage(Math.round(track.scrollLeft / track.clientWidth));
    }
    track.addEventListener("scroll", onScroll, { passive: true });
    return () => track.removeEventListener("scroll", onScroll);
  }, [totalCount]);

  const totalPages = Math.max(1, Math.ceil(totalCount / visibleCount));

  function scrollToPage(page: number) {
    const track = trackRef.current;
    if (!track) return;
    const clamped = Math.max(0, Math.min(page, totalPages - 1));
    track.scrollTo({ left: clamped * track.clientWidth, behavior: "smooth" });
    setActivePage(clamped);
  }

  // Autoplay: paused on hover, on keyboard focus inside the carousel, while actively touching
  // (see the track's touch handlers below), while the tab is hidden, and entirely for
  // prefers-reduced-motion. `autoplayResetKey` is bumped by every manual navigation (arrows,
  // dots, touch end) so that action always restarts a full interval rather than firing early.
  const prefersReducedMotion = useMediaQuery("(prefers-reduced-motion: reduce)");
  const [isHovered, setIsHovered] = useState(false);
  const [isFocused, setIsFocused] = useState(false);
  const [isTouching, setIsTouching] = useState(false);
  const [isPageHidden, setIsPageHidden] = useState(() => typeof document !== "undefined" && document.hidden);
  const [autoplayResetKey, setAutoplayResetKey] = useState(0);

  useEffect(() => {
    function handleVisibilityChange() {
      setIsPageHidden(document.hidden);
    }
    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => document.removeEventListener("visibilitychange", handleVisibilityChange);
  }, []);

  const autoplayActive =
    autoPlay && !prefersReducedMotion && !isHovered && !isFocused && !isTouching && !isPageHidden && totalPages > 1;

  useEffect(() => {
    if (!autoplayActive) return;
    const id = window.setInterval(() => {
      setActivePage((prev) => {
        const next = (prev + 1) % totalPages;
        const track = trackRef.current;
        if (track) {
          track.scrollTo({ left: next * track.clientWidth, behavior: "smooth" });
        }
        return next;
      });
    }, autoPlayInterval);
    return () => window.clearInterval(id);
  }, [autoplayActive, totalPages, autoPlayInterval, autoplayResetKey]);

  function handleManualNavigate(page: number) {
    scrollToPage(page);
    setAutoplayResetKey((key) => key + 1);
  }

  function handleTouchEnd() {
    setIsTouching(false);
    setAutoplayResetKey((key) => key + 1);
  }

  function handleBlur(e: FocusEvent<HTMLDivElement>) {
    if (!e.relatedTarget || !e.currentTarget.contains(e.relatedTarget as Node)) {
      setIsFocused(false);
    }
  }

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

  if (totalCount === 0) {
    return null;
  }

  const canGoPrev = activePage > 0;
  const canGoNext = activePage < totalPages - 1;
  const showControls = totalPages > 1;
  const showDots = showControls && totalPages <= MAX_DOTS;
  const showPositionReadout = showControls && totalPages > MAX_DOTS;
  // Headerless carousels (no title - see the Tuition search page) have nowhere to put the
  // arrows except at the track's own edges, so a header row is never reserved just for them.
  const showEdgeArrows = showControls && !title;

  return (
    <div
      className={rootClassName}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      onFocus={() => setIsFocused(true)}
      onBlur={handleBlur}
    >
      {title && (
        <div className="featured-tuition-carousel__header">
          <h2>{title}</h2>
          {showControls && (
            <div className="featured-tuition-carousel__arrows">
              <button
                type="button"
                className="featured-tuition-carousel__arrow"
                aria-label="Previous featured classes"
                disabled={!canGoPrev}
                onClick={() => handleManualNavigate(activePage - 1)}
              >
                <FaChevronLeft aria-hidden="true" />
              </button>
              <button
                type="button"
                className="featured-tuition-carousel__arrow"
                aria-label="Next featured classes"
                disabled={!canGoNext}
                onClick={() => handleManualNavigate(activePage + 1)}
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
            disabled={!canGoPrev}
            onClick={() => handleManualNavigate(activePage - 1)}
          >
            <FaChevronLeft aria-hidden="true" />
          </button>
        )}

        <div
          className="featured-tuition-carousel__track"
          ref={trackRef}
          onTouchStart={() => setIsTouching(true)}
          onTouchEnd={handleTouchEnd}
          onTouchCancel={handleTouchEnd}
        >
          {items.map((card) => (
            <div className="featured-tuition-carousel__item" key={card.id}>
              {renderItem(card)}
            </div>
          ))}
          {Array.from({ length: placeholderCount }, (_, index) => (
            <div className="featured-tuition-carousel__item" key={`featured-placeholder-${index}`}>
              {renderPlaceholder()}
            </div>
          ))}
        </div>

        {showEdgeArrows && (
          <button
            type="button"
            className="featured-tuition-carousel__edge-arrow featured-tuition-carousel__edge-arrow--next"
            aria-label="Next promotions"
            disabled={!canGoNext}
            onClick={() => handleManualNavigate(activePage + 1)}
          >
            <FaChevronRight aria-hidden="true" />
          </button>
        )}
      </div>

      {showDots && (
        <div className="featured-tuition-carousel__dots" role="tablist" aria-label={`${title ?? "Promotions"} pages`}>
          {Array.from({ length: totalPages }, (_, page) => (
            <button
              key={page}
              type="button"
              role="tab"
              className="featured-tuition-carousel__dot"
              aria-label={`Go to featured page ${page + 1}`}
              aria-selected={page === activePage}
              data-active={page === activePage}
              onClick={() => handleManualNavigate(page)}
            />
          ))}
        </div>
      )}

      {showPositionReadout && (
        <p className="featured-tuition-carousel__position" aria-live="polite">
          {activePage + 1} / {totalPages}
        </p>
      )}
    </div>
  );
}
