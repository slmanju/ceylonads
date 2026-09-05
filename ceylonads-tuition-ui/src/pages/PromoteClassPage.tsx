import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { FaCheckCircle, FaMapMarkerAlt } from "react-icons/fa";
import { getMyAds } from "../api/adsApi";
import { getCompatibleTuitionPromotionPlans, createTuitionPromotion, getMyPromotions } from "../api/promotionApi";
import { useCampaign } from "../campaign/CampaignContext";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { EmptyState } from "../components/EmptyState/EmptyState";
import { Seo } from "../components/Seo/Seo";
import { PromotionPlanCard } from "../components/PromotionPlanCard/PromotionPlanCard";
import type { AdResponse, CompatiblePromotionPlanResponse, PromotionResponse } from "../types/api";
import { formatPrice, formatAdPrice, formatPromotionPrice } from "../utils/formatPrice";
import { formatAdLocations } from "../utils/formatLocations";
import { formatFullDate } from "../utils/formatDate";
import { getApiErrorMessage } from "../utils/apiError";
import { getPromotionDisplay, getPromotionDisplayName, sortByPromotionDisplayOrder } from "../utils/promotionDisplay";
import { formatCampaignDurationLabel } from "../utils/campaignDuration";
import "./PromoteClassPage.css";

const LIVE_STATUSES = new Set(["PENDING_PAYMENT", "PENDING_APPROVAL", "ACTIVE"]);

// My Classes' "Promote"/"Manage Promotion" destination. Reuses the shared CeylonAds promotion
// backend through the Tuition-scoped endpoints (see api/promotionApi.ts) - the same
// promotions/promotion_plans/promotion_slots tables and approval/payment rules ceylonads-ui's
// PromoteAdPage drives, just channel-checked server-side to this tutor's own TUITION listing.
export function PromoteClassPage() {
  const { id } = useParams<{ id: string }>();
  const { campaign } = useCampaign();
  const campaignDurationLabel = campaign ? formatCampaignDurationLabel(campaign.startsAt, campaign.endsAt) : null;

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [ad, setAd] = useState<AdResponse | null>(null);
  const [plans, setPlans] = useState<CompatiblePromotionPlanResponse[]>([]);
  const [currentPromotions, setCurrentPromotions] = useState<PromotionResponse[]>([]);

  const [selectedPlan, setSelectedPlan] = useState<CompatiblePromotionPlanResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [created, setCreated] = useState<PromotionResponse | null>(null);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    setLoading(true);
    setLoadError(null);

    Promise.all([getMyAds(), getCompatibleTuitionPromotionPlans(id), getMyPromotions()])
      .then(([myAds, planResponse, promotionResponse]) => {
        if (cancelled) return;
        const adResponse = myAds.find((a) => String(a.id) === id);
        if (!adResponse) {
          setLoadError("This class could not be found, or it doesn't belong to your account.");
          return;
        }
        if (adResponse.status !== "ACTIVE") {
          setLoadError("Only active classes can be promoted.");
          return;
        }
        setAd(adResponse);
        setPlans(planResponse);
        setCurrentPromotions(promotionResponse.filter((p) => p.adId === adResponse.id));
      })
      .catch((err) => {
        if (!cancelled) setLoadError(getApiErrorMessage(err, "Could not load promotion options for this class."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [id]);

  const handleConfirm = async () => {
    if (!ad || !selectedPlan) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const promotion = await createTuitionPromotion(ad.id, selectedPlan.plan.id);
      setCreated(promotion);
    } catch (err) {
      setSubmitError(getApiErrorMessage(err, "Could not create this promotion."));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="container promote-class-page">
        <LoadingState label="Loading promotion options…" />
      </div>
    );
  }

  if (loadError || !ad) {
    return (
      <div className="container promote-class-page">
        <ErrorState title="Can't request this promotion" message={loadError ?? "This class is unavailable."} />
        <p className="promote-class-page__back">
          <Link to="/my-ads" className="btn btn-primary">
            Back to My Classes
          </Link>
        </p>
      </div>
    );
  }

  if (created) {
    const isPendingApproval = created.status === "PENDING_APPROVAL";
    const isActive = created.status === "ACTIVE";
    const isPendingPayment = created.status === "PENDING_PAYMENT";
    return (
      <div className="container promote-class-page">
        <Seo title="Promotion Requested" noindex />
        <div className="promote-class-page__success">
          <FaCheckCircle className="promote-class-page__success-icon" aria-hidden="true" />
          <h1>Promotion request submitted</h1>
          <p>
            {isPendingPayment && (
              <>
                Your promotion request for <strong>{ad.title}</strong> has been submitted. Our team will contact you
                to arrange payment, and your promotion will go live once approved.
              </>
            )}
            {isPendingApproval && (
              <>
                Your promotion request for <strong>{ad.title}</strong> has been submitted and is awaiting approval.
                No payment is required for this plan.
              </>
            )}
            {isActive && (
              <>
                Your promotion for <strong>{ad.title}</strong> is now active.
              </>
            )}
          </p>
          <div className="promote-class-page__success-actions">
            <Link to="/my-ads" className="btn btn-primary">
              Back to My Classes
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const adLocationLabel = formatAdLocations(ad.locations);
  const livePromotions = currentPromotions.filter((p) => LIVE_STATUSES.has(p.status));

  return (
    <div className="container promote-class-page">
      <Seo title="Promote Class" noindex />
      <h1 className="promote-class-page__title">Promote This Class</h1>

      <div className="promote-class-page__ad-summary">
        <p className="promote-class-page__ad-title">{ad.title}</p>
        <p className="promote-class-page__ad-price">{formatAdPrice(ad.price)}</p>
        <p className="promote-class-page__ad-meta">
          <FaMapMarkerAlt aria-hidden="true" /> {adLocationLabel ? `${adLocationLabel} · ${ad.category}` : ad.category}
        </p>
      </div>

      {livePromotions.length > 0 && (
        <div className="promote-class-page__current">
          <h2 className="promote-class-page__section-title">Current promotions</h2>
          <ul className="promote-class-page__current-list">
            {livePromotions.map((promotion) => (
              <li key={promotion.id}>
                <span className="promote-class-page__current-name">
                  {getPromotionDisplayName(promotion.promotionPlanCode, promotion.promotionPlanName)}
                </span>
                <span className={`promote-class-page__current-status promote-class-page__current-status--${promotion.status.toLowerCase()}`}>
                  {promotion.status.replace("_", " ")}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {!selectedPlan && (
        <>
          <h2 className="promote-class-page__section-title">Choose a promotion</h2>

          {plans.length === 0 ? (
            <EmptyState title="No promotion plans are available right now." message="Please check back later." />
          ) : (
            <div className="promote-class-page__plans">
              {sortByPromotionDisplayOrder(plans, (p) => p.plan.code).map((compatiblePlan) => (
                <PromotionPlanCard
                  key={compatiblePlan.plan.id}
                  plan={compatiblePlan.plan}
                  available={compatiblePlan.available}
                  ctaLabel="Select"
                  unavailableCtaLabel="Full"
                  unavailableLabel="Fully booked right now"
                  onSelect={() => setSelectedPlan(compatiblePlan)}
                  campaignDurationLabel={campaignDurationLabel}
                />
              ))}
            </div>
          )}
        </>
      )}

      {selectedPlan && (
        <div className="promote-class-page__confirm">
          <h2 className="promote-class-page__section-title">Confirm your promotion request</h2>

          {submitError && (
            <p className="promote-class-page__error" role="alert">
              {submitError}
            </p>
          )}

          <dl className="promote-class-page__confirm-details">
            <div>
              <dt>Plan</dt>
              <dd>{getPromotionDisplay(selectedPlan.plan).displayName}</dd>
            </div>
            <div>
              <dt>Where it appears</dt>
              <dd>{getPromotionDisplay(selectedPlan.plan).whereItAppears}</dd>
            </div>
            <div>
              <dt>Promotion duration</dt>
              <dd>{selectedPlan.plan.durationDays} days</dd>
            </div>
            <div>
              <dt>Price</dt>
              <dd>
                {formatPromotionPrice(selectedPlan.plan.currentPrice)}
                {selectedPlan.plan.discounted && (
                  <span className="promote-class-page__confirm-normal-price">
                    {" "}
                    (normal {formatPrice(selectedPlan.plan.price)})
                  </span>
                )}
              </dd>
            </div>
          </dl>

          {selectedPlan.plan.discounted && selectedPlan.plan.currentPrice === 0 && selectedPlan.plan.campaignEndsAt && (
            <p className="promote-class-page__hint">
              This free launch price is available for any promotion you request until{" "}
              {formatFullDate(selectedPlan.plan.campaignEndsAt)}. Once it starts, your own promotion will still run
              for its full {selectedPlan.plan.durationDays} days.
            </p>
          )}

          <p className="promote-class-page__hint">
            Your class will remain active for the full promotion period - if it's close to its own free-listing
            expiry, we automatically extend it so the promotion never outlives the listing.
          </p>

          {selectedPlan.plan.paymentRequired && selectedPlan.plan.currentPrice > 0 && (
            <p className="promote-class-page__hint">
              This plan requires payment. After you submit this request, our team will contact you to arrange
              payment before the promotion goes live.
            </p>
          )}

          <div className="promote-class-page__confirm-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setSelectedPlan(null)}
              disabled={submitting}
            >
              Back
            </button>
            <button type="button" className="btn btn-accent" onClick={handleConfirm} disabled={submitting}>
              {submitting ? "Please wait…" : "Submit Promotion Request"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
