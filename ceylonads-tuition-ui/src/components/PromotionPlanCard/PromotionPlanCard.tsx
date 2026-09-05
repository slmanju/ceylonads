import { getPromotionDisplay } from "../../utils/promotionDisplay";
import { formatPrice, formatPromotionPrice } from "../../utils/formatPrice";
import type { PromotionPlanResponse } from "../../types/api";
import "./PromotionPlanCard.css";

interface PromotionPlanCardProps {
  plan: PromotionPlanResponse;
  available: boolean;
  ctaLabel: string;
  unavailableCtaLabel: string;
  unavailableLabel: string;
  onSelect: () => void;
  // Rough campaign-length label (e.g. "3-month"), from formatCampaignDurationLabel - shown only
  // next to a FREE launch price so a customer never reads "FREE" and the 30-day plan duration
  // right next to each other and assumes the free campaign itself is only 30 days long. Omit while
  // the campaign hasn't loaded yet; the card falls back to campaign-agnostic wording.
  campaignDurationLabel?: string | null;
}

// Shared purchase-flow card for a single promotion plan - used by both PricingPage (public,
// unauthenticated browsing) and PromoteClassPage ("My Classes" purchase flow) so the placement
// name/wording/hierarchy a customer sees is identical in both places. Deliberately leads with
// placement and benefit before price, and never renders a raw slot capacity count (see
// getPromotionDisplay/CLAUDE.md "Promotions" - only a plain available/unavailable CTA state).
export function PromotionPlanCard({
  plan,
  available,
  ctaLabel,
  unavailableCtaLabel,
  unavailableLabel,
  onSelect,
  campaignDurationLabel,
}: PromotionPlanCardProps) {
  const display = getPromotionDisplay(plan);
  const Icon = display.icon;
  const isFreeLaunch = plan.discounted && plan.currentPrice === 0;

  return (
    <div className={`promo-plan-card${display.recommended ? " promo-plan-card--recommended" : ""}`}>
      {display.recommended && <span className="promo-plan-card__badge">Recommended</span>}

      <Icon className="promo-plan-card__icon" aria-hidden="true" />
      <h3 className="promo-plan-card__name">{display.displayName}</h3>

      <p className="promo-plan-card__where-label">Where it appears</p>
      <p className="promo-plan-card__where">{display.whereItAppears}</p>
      <p className="promo-plan-card__benefit">{display.benefit}</p>

      <div className="promo-plan-card__price-block">
        {isFreeLaunch ? (
          <>
            <span className="promo-plan-card__offer-label">Launch offer</span>
            <p className="promo-plan-card__price promo-plan-card__price--free">FREE</p>
            <p className="promo-plan-card__duration">{plan.durationDays}-day promotion</p>
            <p className="promo-plan-card__campaign-note">
              Free during our {campaignDurationLabel ? `${campaignDurationLabel} ` : ""}launch period
            </p>
            <p className="promo-plan-card__price-normal">Normally {formatPrice(plan.price)}</p>
          </>
        ) : plan.discounted ? (
          <>
            {plan.campaignName && <span className="promo-plan-card__offer-label">{plan.campaignName}</span>}
            <p className="promo-plan-card__duration">{plan.durationDays}-day promotion</p>
            <p className="promo-plan-card__price">{formatPromotionPrice(plan.currentPrice)}</p>
            <p className="promo-plan-card__price-normal">Normally {formatPrice(plan.price)}</p>
          </>
        ) : (
          <>
            <p className="promo-plan-card__duration">{plan.durationDays}-day promotion</p>
            <p className="promo-plan-card__price">{formatPrice(plan.price)}</p>
          </>
        )}
      </div>

      {!available && <p className="promo-plan-card__unavailable">{unavailableLabel}</p>}

      <button
        type="button"
        className="btn btn-accent promo-plan-card__cta"
        disabled={!available}
        onClick={onSelect}
      >
        {available ? ctaLabel : unavailableCtaLabel}
      </button>
    </div>
  );
}
