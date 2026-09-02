import { Link } from "react-router-dom";
import { FaBullhorn } from "react-icons/fa";
import "./PromotionCta.css";

interface PromotionCtaProps {
  categoryName?: string;
  className?: string;
}

export function PromotionCta({ categoryName, className }: PromotionCtaProps) {
  return (
    <div className={`promotion-cta ${className ?? ""}`}>
      <div className="promotion-cta__header">
        <FaBullhorn aria-hidden="true" className="promotion-cta__icon" />
        <span className="promotion-cta__eyebrow">Advertisement</span>
      </div>
      <p className="promotion-cta__title">Your ad could be here</p>
      {categoryName && (
        <p className="promotion-cta__subtitle">
          Reach buyers browsing
          <br />
          {categoryName}
        </p>
      )}
      <Link to="/my-ads" className="promotion-cta__link">
        Promote your ad →
      </Link>
    </div>
  );
}
