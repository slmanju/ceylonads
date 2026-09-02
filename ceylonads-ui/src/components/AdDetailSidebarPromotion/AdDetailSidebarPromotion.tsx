import { PromotionCta } from "../PromotionCta/PromotionCta";

interface AdDetailSidebarPromotionProps {
  categoryName: string;
  className?: string;
}

// No AD_DETAIL_SIDEBAR placement exists in the promotion system yet (only
// HOME_FEATURED/HOME_BANNER/CATEGORY_FEATURED/CATEGORY_BANNER/TOP_SEARCH,
// and no category-filtered "get promoted ads" endpoint), so there is no
// active promoted content to fetch here. Once that placement/endpoint
// exists, branch here: render the promoted ad when one is active, and
// keep the PromotionCta placeholder as the fallback otherwise.
export function AdDetailSidebarPromotion({ categoryName, className }: AdDetailSidebarPromotionProps) {
  return <PromotionCta categoryName={categoryName} className={className} />;
}
