import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { useCampaign } from "../campaign/CampaignContext";
import { useCampaignCta } from "../campaign/useCampaignCta";
import { listTuitionPromotionPlans } from "../api/promotionApi";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { Seo } from "../components/Seo/Seo";
import { PromotionPlanCard } from "../components/PromotionPlanCard/PromotionPlanCard";
import type { CompatiblePromotionPlanResponse } from "../types/api";
import { formatFullDate } from "../utils/formatDate";
import { sortByPromotionDisplayOrder } from "../utils/promotionDisplay";
import { CURRENT_PROMOTION_PLAN_DURATION_DAYS, formatCampaignDurationLabel } from "../utils/campaignDuration";
import "./PricingPage.css";

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

  const campaignDurationLabel = campaign ? formatCampaignDurationLabel(campaign.startsAt, campaign.endsAt) : null;

  return (
    <div className="pricing-page container">
      <Seo
        title="Promotion Pricing"
        description="Promotion pricing for ezClass tuition classes - see normal prices and any active campaign offers."
      />

      <div className="pricing-page__header">
        <h1 className="pricing-page__title">Promotion Pricing</h1>
        <p className="pricing-page__subtitle">
          Choose where you want your class to get extra visibility. During our launch period, all promotions are
          FREE.
        </p>
        <p className="pricing-page__tip">
          Not sure which one to choose? <strong>Search Boost</strong> is a good starting option because it places
          your class higher when students search for matching classes.
        </p>

        {campaign && (
          <div className="pricing-page__campaign">
            <span className="pricing-page__campaign-name">
              {campaignDurationLabel ? `${campaignDurationLabel} Free Launch Promotion` : campaign.name}
            </span>
            <p className="pricing-page__campaign-headline">{campaign.headline}</p>
            {campaign.message && <p className="pricing-page__campaign-message">{campaign.message}</p>}
            {campaign.endsAt && (
              <p className="pricing-page__campaign-ends">
                All eligible promotion placements are free until {formatFullDate(campaign.endsAt)}.
              </p>
            )}
            <p className="pricing-page__campaign-plan-duration">
              Each promotion you choose runs for {CURRENT_PROMOTION_PLAN_DURATION_DAYS} days.
            </p>
          </div>
        )}
      </div>

      {loading && <LoadingState label="Loading promotion pricing…" />}

      {!loading && loadError && <ErrorState title={loadError} onRetry={loadPlans} />}

      {!loading && !loadError && (
        <div className="pricing-page__grid">
          {sortByPromotionDisplayOrder(plans, (p) => p.plan.code).map(({ plan, available }) => (
            <PromotionPlanCard
              key={plan.id}
              plan={plan}
              available={available}
              ctaLabel="Promote a Class"
              unavailableCtaLabel="Sold Out"
              unavailableLabel="Sold out for now"
              onSelect={handlePromoteCta}
              campaignDurationLabel={campaignDurationLabel}
            />
          ))}
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
