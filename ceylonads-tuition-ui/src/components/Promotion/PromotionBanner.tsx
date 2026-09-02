import { FaArrowRight, FaBullhorn } from "react-icons/fa";
import type { TuitionPromotion } from "../../tuition/promotion/model/promotion";
import { PromotionLabelBadge } from "../Badge/Badge";
import { PromotionLink } from "./PromotionLink";
import "./PromotionBanner.css";

interface PromotionBannerProps {
  promotion: TuitionPromotion;
  /** "large" for the homepage top banner, "compact" for the detail-page banner. */
  size?: "large" | "compact";
}

export function PromotionBanner({ promotion, size = "large" }: PromotionBannerProps) {
  return (
    <PromotionLink target={promotion.target} className={`promotion-banner promotion-banner--${size}`}>
      <span className="promotion-banner__icon" aria-hidden="true">
        {promotion.imageUrl ? <img src={promotion.imageUrl} alt="" className="promotion-banner__image" /> : <FaBullhorn />}
      </span>
      <div className="promotion-banner__body">
        <PromotionLabelBadge label={promotion.label} />
        <p className="promotion-banner__title">{promotion.title}</p>
        {promotion.subtitle && <p className="promotion-banner__subtitle">{promotion.subtitle}</p>}
      </div>
      {promotion.ctaLabel && (
        <span className="promotion-banner__cta">
          {promotion.ctaLabel} <FaArrowRight aria-hidden="true" />
        </span>
      )}
    </PromotionLink>
  );
}
