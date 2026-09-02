import type { TuitionPromotion } from "../../tuition/promotion/model/promotion";
import { PromotionSideCard } from "./PromotionSideCard";
import { PromotionSelfAd } from "./PromotionSelfAd";
import "./PromotionHomeRail.css";

interface PromotionHomeRailProps {
  promotion?: TuitionPromotion;
}

// The real backend inventory for this placement (TUITION_HOME_LATEST_RIGHT, "Homepage Spotlight")
// is a single right-of-Latest-Classes slot, not a multi-position rail - see HomePage.tsx, which
// fetches it via GET /api/tuition/featured?slot=TUITION_HOME_LATEST_RIGHT. With no active
// promotion, this falls back to the UI-owned PromotionSelfAd ("Advertise Here") card so the
// section never collapses to a blank column.
export function PromotionHomeRail({ promotion }: PromotionHomeRailProps) {
  return (
    <aside className="promotion-home-rail" aria-label="Advertisements">
      {promotion ? <PromotionSideCard promotion={promotion} /> : <PromotionSelfAd />}
    </aside>
  );
}
