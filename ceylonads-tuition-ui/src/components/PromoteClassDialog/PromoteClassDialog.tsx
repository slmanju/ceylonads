import { useEffect, useState } from "react";
import { promoteTuitionAd } from "../../api/adminTuitionApi";
import { listPromotionPlans } from "../../api/adminTuitionPromotionApi";
import { formatPrice, formatPromotionPrice } from "../../utils/formatPrice";
import { getApiErrorMessage } from "../../utils/apiError";
import { getPromotionDisplay, getPromotionDisplayName } from "../../utils/promotionDisplay";
import type { AdResponse, PromotionPlanResponse, PromotionResponse } from "../../types/api";
import "./PromoteClassDialog.css";

interface PromoteClassDialogProps {
  ad: AdResponse;
  open: boolean;
  onCancel: () => void;
  onPromoted: (promotion: PromotionResponse) => void;
}

// Admin-initiated "Promote Class": lets an admin activate one of the current ezClass Tuition
// promotion products for an eligible class directly, without impersonating the owner or routing
// through the owner's own pending-review purchase flow (see PromotionService on the backend -
// this always activates immediately, since the admin's action here is itself the approval).
export function PromoteClassDialog({ ad, open, onCancel, onPromoted }: PromoteClassDialogProps) {
  const [plans, setPlans] = useState<PromotionPlanResponse[] | null>(null);
  const [plansError, setPlansError] = useState<string | null>(null);
  const [selectedPlanId, setSelectedPlanId] = useState<number | "">("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setSelectedPlanId("");
    setSubmitError(null);
    setPlans(null);
    setPlansError(null);
    listPromotionPlans()
      .then(setPlans)
      .catch((err) => setPlansError(getApiErrorMessage(err, "Could not load promotion plans.")));
  }, [open]);

  if (!open) return null;

  const selectedPlan = plans?.find((p) => p.id === selectedPlanId) ?? null;

  const handleConfirm = async () => {
    if (selectedPlanId === "") return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const promotion = await promoteTuitionAd(ad.id, selectedPlanId);
      onPromoted(promotion);
    } catch (err) {
      setSubmitError(getApiErrorMessage(err, "Could not promote this class."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="promote-dialog" role="dialog" aria-modal="true" aria-label="Promote class">
      <button type="button" className="promote-dialog__backdrop" aria-label="Cancel" onClick={onCancel} />
      <div className="promote-dialog__panel">
        <h2 className="promote-dialog__title">Promote Class</h2>

        <div className="promote-dialog__class">
          {ad.media[0] ? (
            <img src={ad.media[0].url} alt="" className="promote-dialog__thumb" />
          ) : (
            <div className="promote-dialog__thumb promote-dialog__thumb--placeholder" aria-hidden="true" />
          )}
          <div className="promote-dialog__class-info">
            <p className="promote-dialog__class-title">{ad.title}</p>
            <p className="promote-dialog__class-meta">{ad.seller.displayName}</p>
            {ad.expiresAt && (
              <p className="promote-dialog__class-meta">
                Expires {new Date(ad.expiresAt).toLocaleDateString("en-LK", { year: "numeric", month: "short", day: "numeric" })}
              </p>
            )}
          </div>
        </div>

        {plansError && (
          <p className="promote-dialog__error" role="alert">
            {plansError}
          </p>
        )}

        {!plansError && (
          <label className="promote-dialog__field">
            Promotion Plan
            <select
              value={selectedPlanId}
              disabled={!plans}
              onChange={(e) => setSelectedPlanId(e.target.value ? Number(e.target.value) : "")}
            >
              <option value="">{plans ? "Select a plan…" : "Loading…"}</option>
              {plans?.map((plan) => (
                <option key={plan.id} value={plan.id}>
                  {getPromotionDisplayName(plan.code, plan.name)}
                </option>
              ))}
            </select>
          </label>
        )}

        {selectedPlan && (
          <dl className="promote-dialog__plan-details">
            <div>
              <dt>Placement</dt>
              <dd>{getPromotionDisplay(selectedPlan).whereItAppears}</dd>
            </div>
            <div>
              <dt>Duration</dt>
              <dd>{selectedPlan.durationDays} days</dd>
            </div>
            <div>
              <dt>Base Price</dt>
              <dd>{formatPrice(selectedPlan.price)}</dd>
            </div>
            <div>
              <dt>{selectedPlan.discounted ? "Current Price" : "Price"}</dt>
              <dd className={selectedPlan.discounted ? "promote-dialog__price--discounted" : undefined}>
                {formatPromotionPrice(selectedPlan.currentPrice)}
              </dd>
            </div>
            {selectedPlan.campaignName && (
              <div>
                <dt>Campaign</dt>
                <dd>{selectedPlan.campaignName}</dd>
              </div>
            )}
          </dl>
        )}

        {submitError && (
          <p className="promote-dialog__error" role="alert">
            {submitError}
          </p>
        )}

        <div className="promote-dialog__actions">
          <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleConfirm}
            disabled={submitting || selectedPlanId === ""}
          >
            {submitting ? "Promoting…" : "Confirm"}
          </button>
        </div>
      </div>
    </div>
  );
}
