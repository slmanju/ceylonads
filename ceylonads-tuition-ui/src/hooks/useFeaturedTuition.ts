import { useEffect, useState } from "react";
import { tuitionRepository } from "../tuition/api/tuitionApi";
import type { TuitionFeaturedCardResponse } from "../types/api";

export interface UseFeaturedTuitionResult {
  featured: TuitionFeaturedCardResponse[];
  loading: boolean;
}

// Backs any fixed TUITION_FEATURED-shaped carousel: one request to the isolated GET
// /api/tuition/featured endpoint, independent of the main class search (see ceylonads-api's
// TuitionFeaturedService). Defaults to the homepage/search carousels' shared TUITION_FEATURED
// slot; pass `slot` to read a different, independently-sellable slot instead (e.g.
// TUITION_DETAIL_TOP_CAROUSEL for the class detail page's top carousel - see ClassDetailPage.tsx).
// `excludeAdId` drops one ad (e.g. the listing currently being viewed) from the result.
export function useFeaturedTuition(
  size = 10,
  { slot, excludeAdId }: { slot?: string; excludeAdId?: number } = {},
): UseFeaturedTuitionResult {
  const [featured, setFeatured] = useState<TuitionFeaturedCardResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    let cancelled = false;
    setLoading(true);

    tuitionRepository
      .getFeaturedTuition({ size, slot, excludeAdId }, controller.signal)
      .then((data) => {
        if (!cancelled) setFeatured(data);
      })
      .catch(() => {
        // Featured tuition is a nice-to-have promotional section - fail quietly rather than
        // showing an error state for a non-essential, promotion-only carousel.
      })
      .finally(() => {
        // A stale/aborted request (e.g. StrictMode's mount/unmount/remount, or size/slot changing
        // before the previous fetch resolves) must not flip loading back to false - that would
        // briefly render the carousel as "loaded but empty" against the still-empty `featured`
        // array, and the placeholder-only DOM that produces gets replaced a moment later when the
        // real request resolves, which shifts scroll position via the browser's CSS scroll
        // anchoring and hides the just-arrived real card.
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [size, slot, excludeAdId]);

  return { featured, loading };
}
