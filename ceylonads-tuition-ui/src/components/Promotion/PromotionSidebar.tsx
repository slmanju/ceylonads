import type { TuitionPromotion } from "../../tuition/promotion/model/promotion";
import { PromotionSideCard } from "./PromotionSideCard";
import { PromotionSelfAd } from "./PromotionSelfAd";
import "./PromotionSidebar.css";

interface PromotionSidebarProps {
  top?: TuitionPromotion;
  middle?: TuitionPromotion;
  bottom?: TuitionPromotion;
}

// Dedicated tuition-search promotions column (TUITION_SEARCH_SIDEBAR_TOP/MIDDLE/BOTTOM) - kept
// visually and structurally separate from the organic ClassGrid so sponsored content never affects
// the organic result count or pagination. Renders whichever slots are actually filled (a slot with
// no eligible promotion is simply skipped, never an empty gap). The real backend
// (GET /api/tuition/promotions) can legitimately return no active promotion for any slot, so when
// none are filled this falls back to the same house ad PromotionHomeRail uses, rather than
// collapsing the column - the sidebar always has something to show.
export function PromotionSidebar({ top, middle, bottom }: PromotionSidebarProps) {
  const items = [top, middle, bottom].filter((promotion): promotion is TuitionPromotion => Boolean(promotion));

  if (items.length === 0) {
    return (
      <aside className="promotion-sidebar" aria-label="Advertisement">
        <PromotionSelfAd />
      </aside>
    );
  }

  return (
    <aside className="promotion-sidebar" aria-label="Sponsored">
      <span className="promotion-sidebar__eyebrow">Promotions</span>
      {items.map((promotion) => (
        <PromotionSideCard key={promotion.id} promotion={promotion} />
      ))}
    </aside>
  );
}
