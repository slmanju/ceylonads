import { useEffect, type RefObject } from "react";

// Shared by every popover-style control in the tuition filter bar (Location/Fee/More Filters) so
// each one doesn't reimplement its own outside-click/Escape wiring.
export function useClickOutside(ref: RefObject<HTMLElement | null>, onOutside: () => void, active: boolean) {
  useEffect(() => {
    if (!active) return;

    function handlePointer(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) onOutside();
    }
    function handleKey(e: KeyboardEvent) {
      if (e.key === "Escape") onOutside();
    }

    document.addEventListener("mousedown", handlePointer);
    document.addEventListener("keydown", handleKey);
    return () => {
      document.removeEventListener("mousedown", handlePointer);
      document.removeEventListener("keydown", handleKey);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active]);
}
