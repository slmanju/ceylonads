import { FaArrowRight, FaBook, FaBullhorn, FaChalkboardTeacher, FaUniversity } from "react-icons/fa";
import type { TuitionPromotion } from "../../tuition/promotion/model/promotion";
import { PromotionLabelBadge } from "../Badge/Badge";
import { PromotionLink } from "./PromotionLink";
import "./PromotionSideCard.css";

const TARGET_ICON = {
  TEACHER_PROFILE: FaChalkboardTeacher,
  INSTITUTE_PROFILE: FaUniversity,
  AD: FaBook,
  EXTERNAL: FaBullhorn,
} as const;

interface PromotionSideCardProps {
  promotion: TuitionPromotion;
}

export function PromotionSideCard({ promotion }: PromotionSideCardProps) {
  const Icon = TARGET_ICON[promotion.target.type];

  return (
    <PromotionLink target={promotion.target} className="promotion-side-card">
      {promotion.imageUrl && <img src={promotion.imageUrl} alt="" className="promotion-side-card__image" />}
      <div className="promotion-side-card__header">
        <span className="promotion-side-card__icon" aria-hidden="true">
          <Icon />
        </span>
        <PromotionLabelBadge label={promotion.label} />
      </div>
      <p className="promotion-side-card__title">{promotion.title}</p>
      {promotion.subtitle && <p className="promotion-side-card__subtitle">{promotion.subtitle}</p>}
      {promotion.ctaLabel && (
        <span className="promotion-side-card__cta">
          {promotion.ctaLabel} <FaArrowRight aria-hidden="true" />
        </span>
      )}
    </PromotionLink>
  );
}
