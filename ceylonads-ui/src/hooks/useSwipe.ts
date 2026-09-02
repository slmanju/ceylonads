import { useRef } from "react";
import type { TouchEvent } from "react";

const SWIPE_THRESHOLD_PX = 40;

// Minimal touch-swipe detector: tracks the horizontal delta of a single touch gesture and fires
// onSwipeLeft/onSwipeRight once it crosses SWIPE_THRESHOLD_PX. Deliberately not a library - a
// carousel only needs a left/right decision, not full gesture physics.
export function useSwipe(onSwipeLeft: () => void, onSwipeRight: () => void) {
  const startX = useRef<number | null>(null);

  const onTouchStart = (event: TouchEvent) => {
    startX.current = event.touches[0]?.clientX ?? null;
  };

  const onTouchEnd = (event: TouchEvent) => {
    if (startX.current === null) return;
    const endX = event.changedTouches[0]?.clientX ?? startX.current;
    const delta = endX - startX.current;
    startX.current = null;

    if (delta <= -SWIPE_THRESHOLD_PX) {
      onSwipeLeft();
    } else if (delta >= SWIPE_THRESHOLD_PX) {
      onSwipeRight();
    }
  };

  return { onTouchStart, onTouchEnd };
}
