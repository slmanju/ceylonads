import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { FaCheckCircle, FaCloudUploadAlt, FaMapMarkerAlt } from "react-icons/fa";
import { getMyAds } from "../api/adsApi";
import { getCompatiblePromotionPlans, createPromotion } from "../api/promotionApi";
import { getBankTransferDetails, getMyPayments, uploadPaymentReceipt } from "../api/paymentApi";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { EmptyState } from "../components/EmptyState/EmptyState";
import type { AdResponse, BankTransferDetailsResponse, CompatiblePromotionPlanResponse, PromotionResponse } from "../types/api";
import { formatPrice, formatAdPrice } from "../utils/formatPrice";
import { formatAdLocations } from "../utils/formatLocations";
import { getApiErrorMessage } from "../utils/apiError";
import "./PromoteAdPage.css";

const PROOF_CONTENT_TYPES = ["image/jpeg", "image/png", "image/webp"];

// This is the single "Request Promotion" workflow reached from both My Ads -> Promote (ad
// preselected via the :id route param) and My Promotions -> Request Promotion (no id; the
// customer picks from their own active ads first). Never fork this into two implementations.
export function PromoteAdPage() {
  const { id } = useParams<{ id: string }>();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [eligibleAds, setEligibleAds] = useState<AdResponse[]>([]);
  const [pickedAdId, setPickedAdId] = useState("");
  const [ad, setAd] = useState<AdResponse | null>(null);
  const [plans, setPlans] = useState<CompatiblePromotionPlanResponse[]>([]);
  const [plansLoading, setPlansLoading] = useState(false);

  const [selectedPlan, setSelectedPlan] = useState<CompatiblePromotionPlanResponse | null>(null);
  const [bankDetails, setBankDetails] = useState<BankTransferDetailsResponse | null>(null);
  const [proofFile, setProofFile] = useState<File | null>(null);
  const [proofFileError, setProofFileError] = useState<string | null>(null);

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [proofWarning, setProofWarning] = useState<string | null>(null);
  const [created, setCreated] = useState<PromotionResponse | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setLoadError(null);
    setAd(null);
    setEligibleAds([]);
    setPickedAdId("");
    setPlans([]);
    setSelectedPlan(null);

    getMyAds()
      .then(async (ads) => {
        if (id) {
          const match = ads.find((a) => String(a.id) === id);
          if (!match) {
            if (!cancelled) setLoadError("This ad could not be found, or it doesn't belong to your account.");
            return;
          }
          if (match.status !== "ACTIVE") {
            if (!cancelled) setLoadError("Only active ads can be promoted.");
            return;
          }
          const compatiblePlans = await getCompatiblePromotionPlans(match.id);
          if (cancelled) return;
          setAd(match);
          setPlans(compatiblePlans);
        } else {
          const active = ads.filter((a) => a.status === "ACTIVE");
          if (!cancelled) setEligibleAds(active);
        }
      })
      .catch((err) => {
        if (!cancelled) setLoadError(getApiErrorMessage(err, "Could not load promotion options."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [id]);

  useEffect(() => {
    if (selectedPlan?.plan.paymentRequired) {
      getBankTransferDetails()
        .then(setBankDetails)
        .catch(() => setBankDetails(null));
    } else {
      setBankDetails(null);
    }
  }, [selectedPlan]);

  const handlePickAd = async () => {
    const match = eligibleAds.find((a) => String(a.id) === pickedAdId);
    if (!match) return;
    setPlansLoading(true);
    setLoadError(null);
    try {
      const compatiblePlans = await getCompatiblePromotionPlans(match.id);
      setAd(match);
      setPlans(compatiblePlans);
    } catch (err) {
      setLoadError(getApiErrorMessage(err, "Could not load promotion options for this ad."));
    } finally {
      setPlansLoading(false);
    }
  };

  const handleProofFileChange = (file: File) => {
    if (!PROOF_CONTENT_TYPES.includes(file.type)) {
      setProofFileError("Please choose a JPEG, PNG, or WEBP image.");
      return;
    }
    setProofFileError(null);
    setProofFile(file);
  };

  const handleConfirm = async () => {
    if (!ad || !selectedPlan) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const promotion = await createPromotion({ adId: ad.id, promotionPlanId: selectedPlan.plan.id });

      let warning: string | null = null;
      if (promotion.status === "PENDING_PAYMENT" && proofFile) {
        try {
          const payments = await getMyPayments();
          const payment = payments.find((p) => p.promotionId === promotion.id);
          if (payment) {
            await uploadPaymentReceipt(payment.id, proofFile);
          }
        } catch (err) {
          // The promotion request itself already succeeded and must not be treated as failed;
          // the customer can retry the upload later from My Payments.
          warning = getApiErrorMessage(
            err,
            "Your promotion request was submitted, but the payment proof upload failed. You can upload it later from My Payments.",
          );
        }
      }

      setProofWarning(warning);
      setCreated(promotion);
    } catch (err) {
      setSubmitError(getApiErrorMessage(err, "Could not create this promotion."));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="container promote-ad-page">
        <LoadingState label="Loading promotion options…" />
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="container promote-ad-page">
        <ErrorState title="Can't request this promotion" message={loadError} />
        <p className="promote-ad-page__back">
          <Link to="/my-ads" className="btn btn-primary">
            Back to My Ads
          </Link>
        </p>
      </div>
    );
  }

  if (created && ad) {
    const isPendingApproval = created.status === "PENDING_APPROVAL";
    const isActive = created.status === "ACTIVE";
    const isPendingPayment = created.status === "PENDING_PAYMENT";
    return (
      <div className="container promote-ad-page">
        <div className="promote-ad-page__success">
          <FaCheckCircle className="promote-ad-page__success-icon" aria-hidden="true" />
          <h1>Promotion request submitted</h1>
          <p>
            {isPendingPayment && (
              <>
                Your promotion request for <strong>{ad.title}</strong> has been submitted.{" "}
                {proofWarning ? proofWarning : "You can complete payment separately or upload payment proof if available."}{" "}
                Your promotion will become active after review and approval.
              </>
            )}
            {isPendingApproval && (
              <>
                Your promotion request for <strong>{ad.title}</strong> has been submitted and is awaiting admin
                approval. No payment is required for this plan.
              </>
            )}
            {isActive && (
              <>
                Your promotion for <strong>{ad.title}</strong> is now active. No payment is required for this plan.
              </>
            )}
          </p>
          <div className="promote-ad-page__success-actions">
            <Link to="/my-promotions" className="btn btn-primary">
              View My Promotions
            </Link>
            <Link to="/my-ads" className="btn btn-secondary">
              Back to My Ads
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (!ad) {
    return (
      <div className="container promote-ad-page">
        <h1 className="promote-ad-page__title">Request Promotion</h1>
        <h2 className="promote-ad-page__section-title">Choose an ad to promote</h2>
        {eligibleAds.length === 0 ? (
          <EmptyState
            title="You don't have any active ads to promote."
            message="Only active ads are eligible for promotion."
            action={
              <Link to="/my-ads" className="btn btn-primary">
                Go to My Ads
              </Link>
            }
          />
        ) : (
          <div className="promote-ad-page__ad-picker">
            <select
              className="promote-ad-page__ad-select"
              value={pickedAdId}
              onChange={(e) => setPickedAdId(e.target.value)}
              aria-label="Select an ad to promote"
            >
              <option value="">Select an ad…</option>
              {eligibleAds.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.title}
                </option>
              ))}
            </select>
            <button
              type="button"
              className="btn btn-primary"
              disabled={!pickedAdId || plansLoading}
              onClick={handlePickAd}
            >
              {plansLoading ? "Loading…" : "Continue"}
            </button>
          </div>
        )}
      </div>
    );
  }

  const adLocationLabel = formatAdLocations(ad.locations);

  return (
    <div className="container promote-ad-page">
      <h1 className="promote-ad-page__title">Request Promotion</h1>

      <div className="promote-ad-page__ad-summary">
        <p className="promote-ad-page__ad-title">{ad.title}</p>
        <p className="promote-ad-page__ad-price">{formatAdPrice(ad.price)}</p>
        <p className="promote-ad-page__ad-meta">
          <FaMapMarkerAlt aria-hidden="true" /> {adLocationLabel ? `${adLocationLabel} · ${ad.category}` : ad.category}
        </p>
      </div>

      {!selectedPlan && (
        <>
          <h2 className="promote-ad-page__section-title">Choose a promotion plan</h2>

          {plans.length === 0 ? (
            <EmptyState title="No promotion plans are available right now." message="Please check back later." />
          ) : (
            <div className="promote-ad-page__plans">
              {plans.map(({ plan, available, remainingCapacity }) => (
                <div key={plan.id} className="promotion-plan-card">
                  <span className="promotion-plan-card__placement">{plan.slotName}</span>
                  <p className="promotion-plan-card__name">{plan.name}</p>
                  <p className="promotion-plan-card__description">{plan.description}</p>
                  <p className="promotion-plan-card__duration">{plan.durationDays} days</p>
                  <p className="promotion-plan-card__price">{formatPrice(plan.price)}</p>
                  <p className="promotion-plan-card__availability">
                    {available
                      ? `Availability: ${remainingCapacity} of ${plan.slotCapacity} slots available`
                      : "Fully booked right now"}
                  </p>
                  <button
                    type="button"
                    className="btn btn-primary promotion-plan-card__select"
                    disabled={!available}
                    onClick={() => setSelectedPlan({ plan, available, remainingCapacity })}
                  >
                    {available ? "Select" : "Full"}
                  </button>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {selectedPlan && (
        <div className="promote-ad-page__confirm">
          <h2 className="promote-ad-page__section-title">Confirm your promotion request</h2>

          {submitError && (
            <p className="promote-ad-page__error" role="alert">
              {submitError}
            </p>
          )}

          <dl className="promote-ad-page__confirm-details">
            <div>
              <dt>Plan</dt>
              <dd>{selectedPlan.plan.name}</dd>
            </div>
            <div>
              <dt>Placement</dt>
              <dd>{selectedPlan.plan.slotName}</dd>
            </div>
            <div>
              <dt>Duration</dt>
              <dd>{selectedPlan.plan.durationDays} days</dd>
            </div>
            <div>
              <dt>Price</dt>
              <dd>{formatPrice(selectedPlan.plan.price)}</dd>
            </div>
          </dl>

          {selectedPlan.plan.paymentRequired && (
            <div className="promote-ad-page__payment-instructions">
              <h3 className="promote-ad-page__section-title">Payment Instructions</h3>
              {bankDetails ? (
                <>
                  <dl className="promote-ad-page__confirm-details">
                    <div>
                      <dt>Bank</dt>
                      <dd>{bankDetails.bankName}</dd>
                    </div>
                    <div>
                      <dt>Account Name</dt>
                      <dd>{bankDetails.accountName}</dd>
                    </div>
                    <div>
                      <dt>Account Number</dt>
                      <dd>{bankDetails.accountNumber}</dd>
                    </div>
                    <div>
                      <dt>Branch</dt>
                      <dd>{bankDetails.branch}</dd>
                    </div>
                  </dl>
                  <p className="promote-ad-page__hint">{bankDetails.instructions}</p>
                </>
              ) : (
                <p className="promote-ad-page__hint">
                  Bank transfer details will also be available on the payment page. Cash and other manually
                  arranged payments are accepted too.
                </p>
              )}

              <div className="promote-ad-page__proof">
                <span className="promote-ad-page__field-label">Payment Proof (optional)</span>
                <p className="promote-ad-page__hint">
                  Upload a bank slip if you have already paid. You can also arrange payment separately.
                </p>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  className="visually-hidden"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) handleProofFileChange(file);
                    e.target.value = "";
                  }}
                />
                {proofFileError && (
                  <p className="promote-ad-page__error" role="alert">
                    {proofFileError}
                  </p>
                )}
                {proofFile ? (
                  <div className="promote-ad-page__proof-selected">
                    <span>{proofFile.name}</span>
                    <button type="button" className="btn btn-outline" onClick={() => setProofFile(null)}>
                      Remove
                    </button>
                  </div>
                ) : (
                  <button type="button" className="btn btn-secondary" onClick={() => fileInputRef.current?.click()}>
                    <FaCloudUploadAlt aria-hidden="true" /> Upload Payment Proof
                  </button>
                )}
              </div>
            </div>
          )}

          <div className="promote-ad-page__confirm-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                setSelectedPlan(null);
                setProofFile(null);
                setProofFileError(null);
              }}
              disabled={submitting}
            >
              Back
            </button>
            <button type="button" className="btn btn-primary" onClick={handleConfirm} disabled={submitting}>
              {submitting ? "Please wait…" : "Submit Promotion Request"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
