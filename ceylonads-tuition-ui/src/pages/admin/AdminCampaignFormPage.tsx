import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  createCampaign,
  getCampaign,
  listPromotionPlans,
  updateCampaign,
} from "../../api/adminTuitionPromotionApi";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { getApiErrorMessage } from "../../utils/apiError";
import { campaignStatusLabel, classifyCampaign, isEditRestricted } from "../../utils/campaignStatus";
import type { PricingType, PromotionPlanResponse } from "../../types/api";
import "./AdminCampaignFormPage.css";

// Formats an ISO Instant as the value a <input type="datetime-local"> expects (local time, no
// seconds/zone) - and the reverse conversion happens implicitly since the browser gives back the
// same format, which we convert to a real Instant string on submit via new Date(value).toISOString().
function toDatetimeLocal(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function AdminCampaignFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [plans, setPlans] = useState<PromotionPlanResponse[]>([]);
  const [historicalPlanIds, setHistoricalPlanIds] = useState<Set<number>>(new Set());
  const [readOnly, setReadOnly] = useState(false);
  const [readOnlyReason, setReadOnlyReason] = useState<string | null>(null);

  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [pricingType, setPricingType] = useState<PricingType>("PERCENTAGE_DISCOUNT");
  const [discountPercent, setDiscountPercent] = useState(50);
  const [fixedPrice, setFixedPrice] = useState(0);
  const [minimumPrice, setMinimumPrice] = useState<number | "">("");
  const [startsAt, setStartsAt] = useState("");
  const [endsAt, setEndsAt] = useState("");
  const [selectedPlanIds, setSelectedPlanIds] = useState<number[]>([]);
  const [active, setActive] = useState(true);
  const [customerVisible, setCustomerVisible] = useState(false);
  const [showBanner, setShowBanner] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [headline, setHeadline] = useState("");
  const [message, setMessage] = useState("");
  const [ctaLabel, setCtaLabel] = useState("");

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    Promise.all([listPromotionPlans(), isEdit && id ? getCampaign(id) : Promise.resolve(null)])
      .then(async ([currentPlans, campaign]) => {
        let planList = currentPlans;
        if (campaign) {
          setCode(campaign.code);
          setName(campaign.name);
          setDescription(campaign.description);
          setPricingType(campaign.pricingType);
          if (campaign.discountPercent != null) setDiscountPercent(campaign.discountPercent);
          if (campaign.fixedPrice != null) setFixedPrice(campaign.fixedPrice);
          setMinimumPrice(campaign.minimumPrice ?? "");
          setStartsAt(toDatetimeLocal(campaign.startsAt));
          setEndsAt(toDatetimeLocal(campaign.endsAt));
          setSelectedPlanIds(campaign.planIds);
          setActive(campaign.active);
          setCustomerVisible(campaign.customerVisible);
          setShowBanner(campaign.showBanner);
          setShowModal(campaign.showModal);
          setHeadline(campaign.headline ?? "");
          setMessage(campaign.message ?? "");
          setCtaLabel(campaign.ctaLabel ?? "");

          const status = classifyCampaign(campaign);
          if (isEditRestricted(status)) {
            setReadOnly(true);
            setReadOnlyReason(
              `This campaign is ${campaignStatusLabel(status).toLowerCase()} and is shown read-only to avoid ` +
                "accidentally reactivating it with outdated pricing.",
            );
          }

          // A campaign created before the current-catalog restriction existed may still map a
          // retired plan - merge those in (clearly labeled) so the selector never silently hides
          // an existing mapping, without offering them as a choice for brand-new campaigns.
          const missingIds = campaign.planIds.filter((pid) => !currentPlans.some((p) => p.id === pid));
          if (missingIds.length > 0) {
            const allPlans = await listPromotionPlans("ALL");
            const historical = allPlans.filter((p) => missingIds.includes(p.id));
            planList = [...currentPlans, ...historical];
            setHistoricalPlanIds(new Set(historical.map((p) => p.id)));
          }
        }
        setPlans(planList);
      })
      .catch((err) => setError(getApiErrorMessage(err, "Could not load this campaign.")))
      .finally(() => setLoading(false));
  }, [id, isEdit]);

  const togglePlan = (planId: number) => {
    setSelectedPlanIds((current) =>
      current.includes(planId) ? current.filter((p) => p !== planId) : [...current, planId],
    );
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    if (selectedPlanIds.length === 0) {
      setError("Select at least one promotion plan for this campaign.");
      return;
    }
    if (!startsAt || !endsAt) {
      setError("Please set both a start and end date.");
      return;
    }
    const startsIso = new Date(startsAt).toISOString();
    const endsIso = new Date(endsAt).toISOString();
    if (new Date(endsIso) <= new Date(startsIso)) {
      setError("End date must be after the start date.");
      return;
    }
    if (customerVisible && (!headline.trim() || !message.trim() || !ctaLabel.trim())) {
      setError("A customer-visible campaign needs a headline, message, and CTA label.");
      return;
    }

    setSubmitting(true);
    try {
      const shared = {
        name: name.trim(),
        description: description.trim(),
        discountPercent: pricingType === "PERCENTAGE_DISCOUNT" ? discountPercent : undefined,
        fixedPrice: pricingType === "FIXED_PRICE" ? fixedPrice : undefined,
        minimumPrice: minimumPrice === "" ? undefined : Number(minimumPrice),
        startsAt: startsIso,
        endsAt: endsIso,
        planIds: selectedPlanIds,
        customerVisible,
        showBanner,
        showModal,
        headline: headline.trim() || undefined,
        message: message.trim() || undefined,
        ctaLabel: ctaLabel.trim() || undefined,
      };

      if (isEdit && id) {
        await updateCampaign(id, { ...shared, active });
      } else {
        await createCampaign({
          ...shared,
          code: code.trim(),
          sourceChannel: "TUITION",
          pricingType,
        });
      }
      navigate("/admin/tuition/campaigns");
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not save this campaign."));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingState label="Loading…" />;

  return (
    <div className="tuition-admin-campaign-form">
      <Link to="/admin/tuition/campaigns" className="tuition-admin-campaign-form__back">
        ← Back to Promotion Campaigns
      </Link>
      <h1>{isEdit ? "Edit Campaign" : "New Campaign"}</h1>

      {readOnlyReason && (
        <p className="tuition-admin-campaign-form__readonly-banner" role="status">
          {readOnlyReason}
        </p>
      )}

      {error && (
        <p className="tuition-admin-campaign-form__error" role="alert">
          {error}
        </p>
      )}

      <fieldset disabled={readOnly} className="tuition-admin-campaign-form__fieldset">
        <form onSubmit={handleSubmit} className="tuition-admin-campaign-form__form">
          <div className="tuition-admin-campaign-form__field">
            <label htmlFor="campaign-code">Code</label>
            {isEdit ? (
              <p className="tuition-admin-campaign-form__hint">{code} (immutable after creation)</p>
            ) : (
              <input id="campaign-code" type="text" value={code} onChange={(e) => setCode(e.target.value)} required />
            )}
          </div>

          <div className="tuition-admin-campaign-form__field">
            <label htmlFor="campaign-name">Name</label>
            <input id="campaign-name" type="text" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>

          <div className="tuition-admin-campaign-form__field">
            <label htmlFor="campaign-description">Description</label>
            <textarea
              id="campaign-description"
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
            />
          </div>

          <div className="tuition-admin-campaign-form__field">
            <label>Pricing Type</label>
            {isEdit ? (
              <p className="tuition-admin-campaign-form__hint">{pricingType} (immutable after creation)</p>
            ) : (
              <div className="tuition-admin-campaign-form__radios">
                <label>
                  <input
                    type="radio"
                    checked={pricingType === "PERCENTAGE_DISCOUNT"}
                    onChange={() => setPricingType("PERCENTAGE_DISCOUNT")}
                  />
                  Percentage Discount
                </label>
                <label>
                  <input
                    type="radio"
                    checked={pricingType === "FIXED_PRICE"}
                    onChange={() => setPricingType("FIXED_PRICE")}
                  />
                  Fixed Price
                </label>
              </div>
            )}
          </div>

          {pricingType === "PERCENTAGE_DISCOUNT" ? (
            <div className="tuition-admin-campaign-form__row">
              <div className="tuition-admin-campaign-form__field">
                <label htmlFor="campaign-discount">Discount %</label>
                <input
                  id="campaign-discount"
                  type="number"
                  min={1}
                  max={100}
                  value={discountPercent}
                  onChange={(e) => setDiscountPercent(Number(e.target.value))}
                  required
                />
              </div>
              <div className="tuition-admin-campaign-form__field">
                <label htmlFor="campaign-min-price">Minimum Price (optional, Rs.)</label>
                <input
                  id="campaign-min-price"
                  type="number"
                  min={0}
                  value={minimumPrice}
                  onChange={(e) => setMinimumPrice(e.target.value === "" ? "" : Number(e.target.value))}
                />
              </div>
            </div>
          ) : (
            <div className="tuition-admin-campaign-form__field">
              <label htmlFor="campaign-fixed-price">Fixed Price (Rs.)</label>
              <input
                id="campaign-fixed-price"
                type="number"
                min={0}
                step="0.01"
                value={fixedPrice}
                onChange={(e) => setFixedPrice(Number(e.target.value))}
                required
              />
            </div>
          )}

          <div className="tuition-admin-campaign-form__row">
            <div className="tuition-admin-campaign-form__field">
              <label htmlFor="campaign-starts">Starts At</label>
              <input
                id="campaign-starts"
                type="datetime-local"
                value={startsAt}
                onChange={(e) => setStartsAt(e.target.value)}
                required
              />
            </div>
            <div className="tuition-admin-campaign-form__field">
              <label htmlFor="campaign-ends">Ends At</label>
              <input
                id="campaign-ends"
                type="datetime-local"
                value={endsAt}
                onChange={(e) => setEndsAt(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="tuition-admin-campaign-form__field">
            <label>Applicable Promotion Plans</label>
            <p className="tuition-admin-campaign-form__hint">Only current Tuition products can be newly selected.</p>
            <div className="tuition-admin-campaign-form__plans">
              {plans.map((plan) => (
                <label key={plan.id} className="tuition-admin-campaign-form__plan-option">
                  <input
                    type="checkbox"
                    checked={selectedPlanIds.includes(plan.id)}
                    disabled={historicalPlanIds.has(plan.id)}
                    onChange={() => togglePlan(plan.id)}
                  />
                  {plan.name} ({plan.slotName})
                  {historicalPlanIds.has(plan.id) && (
                    <span className="tuition-admin-campaign-form__plan-historical"> (Historical)</span>
                  )}
                </label>
              ))}
            </div>
          </div>

          {isEdit && (
            <label className="tuition-admin-campaign-form__inline-checkbox">
              <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
              Active
            </label>
          )}

          <div className="tuition-admin-campaign-form__section">
            <h2>Storefront Presentation</h2>
            <label className="tuition-admin-campaign-form__inline-checkbox">
              <input type="checkbox" checked={customerVisible} onChange={(e) => setCustomerVisible(e.target.checked)} />
              Customer visible
            </label>

            {customerVisible && (
              <>
                <label className="tuition-admin-campaign-form__inline-checkbox">
                  <input type="checkbox" checked={showBanner} onChange={(e) => setShowBanner(e.target.checked)} />
                  Show banner
                </label>
                <label className="tuition-admin-campaign-form__inline-checkbox">
                  <input type="checkbox" checked={showModal} onChange={(e) => setShowModal(e.target.checked)} />
                  Show modal
                </label>

                <div className="tuition-admin-campaign-form__field">
                  <label htmlFor="campaign-headline">Headline</label>
                  <input id="campaign-headline" type="text" value={headline} onChange={(e) => setHeadline(e.target.value)} />
                </div>
                <div className="tuition-admin-campaign-form__field">
                  <label htmlFor="campaign-message">Message</label>
                  <input id="campaign-message" type="text" value={message} onChange={(e) => setMessage(e.target.value)} />
                </div>
                <div className="tuition-admin-campaign-form__field">
                  <label htmlFor="campaign-cta">CTA Label</label>
                  <input id="campaign-cta" type="text" value={ctaLabel} onChange={(e) => setCtaLabel(e.target.value)} />
                </div>
              </>
            )}
          </div>

          {!readOnly && (
            <div className="tuition-admin-campaign-form__actions">
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? "Saving…" : isEdit ? "Save Changes" : "Create Campaign"}
              </button>
            </div>
          )}
        </form>
      </fieldset>
    </div>
  );
}
