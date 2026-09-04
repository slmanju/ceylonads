/** One slot in a composed promotional carousel: either a real backend promotion, or a
 *  frontend-only "Advertise Here" placeholder with a stable, placement-scoped key. */
export type CarouselSlot<T> = { kind: "real"; item: T } | { kind: "placeholder"; key: string };

// Shared composition rule for every ezClass promotional carousel: the composed list must always
// contain more than `visibleCount` items once loaded, so carousel arrows/autoplay always have
// somewhere to go and are never hidden just because paid inventory is sparse (see
// useCircularCarousel). Real promotions always come first; frontend-only "Advertise Here"
// placeholders fill the rest, keyed by `placeholderKeyPrefix` (e.g. "home-featured-placeholder")
// so they stay stable across re-renders/refetches instead of shifting with a bare array index.
// This is presentation-only - it never implies a fixed backend capacity (5, 12, or otherwise);
// once real promotions alone exceed the minimum, no placeholders are added at all.
export function ensureCarouselMinimumItems<T>(
  realItems: T[],
  visibleCount: number,
  placeholderKeyPrefix: string,
): CarouselSlot<T>[] {
  const minimumItems = visibleCount + 1;
  const missing = Math.max(minimumItems - realItems.length, 0);
  const real: CarouselSlot<T>[] = realItems.map((item) => ({ kind: "real", item }));
  const placeholders: CarouselSlot<T>[] = Array.from({ length: missing }, (_, index) => ({
    kind: "placeholder",
    key: `${placeholderKeyPrefix}-${index + 1}`,
  }));
  return [...real, ...placeholders];
}
