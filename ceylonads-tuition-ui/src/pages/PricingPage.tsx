import { useEffect, useState } from "react";
import { FaBolt } from "react-icons/fa";
import { useAuth } from "../auth/AuthContext";
import { useCampaign } from "../campaign/CampaignContext";
import { useCampaignCta } from "../campaign/useCampaignCta";
import { listTuitionPromotionPlans } from "../api/promotionApi";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { Seo } from "../components/Seo/Seo";
import type { CompatiblePromotionPlanResponse } from "../types/api";
import { formatPrice, formatPromotionPrice } from "../utils/formatPrice";
import { formatFullDate } from "../utils/formatDate";
import "./PricingPage.css";

// Plan codes are stable backend identifiers (like a slug - see tuition CLAUDE.md "Categories")
// used only to give the two search-discovery products slightly stronger visual emphasis. No
// price, availability, or campaign logic ever branches on these.
const SEARCH_PLAN_CODES = new Set(["TUITION_SEARCH_TOP_30D", "TUITION_SEARCH_BOOST_30D"]);

export function PricingPage() {
  const { isAuthenticated } = useAuth();
  const { campaign } = useCampaign();
  const handlePromoteCta = useCampaignCta();

  const [plans, setPlans] = useState<CompatiblePromotionPlanResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const loadPlans = () => {
    setLoading(true);
    setLoadError(null);
    listTuitionPromotionPlans()
      .then(setPlans)
      .catch(() => setLoadError("Promotion pricing is temporarily unavailable."))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadPlans();
  }, []);

  return (
    <div className="pricing-page container">
      <Seo
        title="Promotion Pricing"
        description="Promotion pricing for ezClass tuition classes - see normal prices and any active campaign offers."
      />

      <div className="pricing-page__header">
        <h1 className="pricing-page__title">Promotion Pricing</h1>
        <p className="pricing-page__subtitle">Get more visibility for your tuition classes.</p>

        {campaign && (
          <div className="pricing-page__campaign">
            <span className="pricing-page__campaign-name">{campaign.name}</span>
            <p className="pricing-page__campaign-headline">{campaign.headline}</p>
            {campaign.message && <p className="pricing-page__campaign-message">{campaign.message}</p>}
            {campaign.endsAt && (
              <p className="pricing-page__campaign-ends">Offer valid until {formatFullDate(campaign.endsAt)}</p>
            )}
          </div>
        )}
      </div>

      <div className="pricing-page__free-notice">
        <p>
          <strong>Posting your tuition class is free.</strong> Promotion is optional and gives your class additional
          visibility across ezClass.
        </p>
      </div>

      {loading && <LoadingState label="Loading promotion pricing…" />}

      {!loading && loadError && <ErrorState title={loadError} onRetry={loadPlans} />}

      {!loading && !loadError && (
        <div className="pricing-page__grid">
          {plans.map(({ plan, available, remainingCapacity }) => {
            const emphasised = SEARCH_PLAN_CODES.has(plan.code);
            return (
              <div
                key={plan.id}
                className={`pricing-card${emphasised ? " pricing-card--emphasised" : ""}`}
              >
                {emphasised && (
                  <span className="pricing-card__visibility-badge">
                    <FaBolt aria-hidden="true" /> High Visibility
                  </span>
                )}
                <h2 className="pricing-card__name">{plan.name}</h2>
                <p className="pricing-card__description">{plan.description}</p>
                <p className="pricing-card__duration">{plan.durationDays} days</p>

                <div className="pricing-card__price-block">
                  {plan.discounted ? (
                    <>
                      <p className="pricing-card__price-normal-label">Normal price</p>
                      <p className="pricing-card__price-normal-amount">{formatPrice(plan.price)}</p>
                      {plan.campaignName && (
                        <span className="pricing-card__offer-badge">{plan.campaignName}</span>
                      )}
                      <p className="pricing-card__price pricing-card__price--current">
                        {formatPromotionPrice(plan.currentPrice)}
                      </p>
                      <p className="pricing-card__savings">Save {formatPrice(plan.discountAmount)}</p>
                    </>
                  ) : (
                    <p className="pricing-card__price">{formatPrice(plan.price)}</p>
                  )}
                </div>

                <p className="pricing-card__availability">
                  {available ? `${remainingCapacity} of ${plan.slotCapacity} remaining` : "Sold Out"}
                </p>

                <button
                  type="button"
                  className="btn btn-accent pricing-card__cta"
                  disabled={!available}
                  onClick={handlePromoteCta}
                >
                  {available ? "Promote a Class" : "Sold Out"}
                </button>
              </div>
            );
          })}
        </div>
      )}

      <div className="pricing-page__footer-cta">
        <p>Ready to promote your class?</p>
        <button type="button" className="btn btn-secondary" onClick={handlePromoteCta}>
          {isAuthenticated ? "View My Classes" : "Login to Promote"}
        </button>
      </div>
    </div>
  );
}
