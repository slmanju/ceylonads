import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { FaPlus } from "react-icons/fa";
import * as adminPromotionApi from "../../api/adminPromotionApi";
import * as adminPaymentApi from "../../api/adminPaymentApi";
import { listCustomers, listCustomerActiveAds } from "../../api/adminApi";
import { AdminPageHeader } from "../../components/AdminPageHeader/AdminPageHeader";
import { PromotionStatusBadge } from "../../components/PromotionStatusBadge/PromotionStatusBadge";
import { PaymentStatusBadge } from "../../components/PaymentStatusBadge/PaymentStatusBadge";
import { ConfirmDialog } from "../../components/ConfirmDialog/ConfirmDialog";
import { Modal } from "../../components/Modal/Modal";
import { AdminPaymentReviewModal } from "../../components/AdminPaymentReviewModal/AdminPaymentReviewModal";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { useToast } from "../../components/Toast/ToastProvider";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatPrice } from "../../utils/formatPrice";
import { formatDate } from "../../utils/formatDate";
import type {
  AdResponse,
  CustomerResponse,
  PaymentResponse,
  PaymentSummaryResponse,
  PromotionPlanResponse,
  PromotionResponse,
  PromotionSlotResponse,
  PromotionStatus,
} from "../../types/api";
import "./AdminForm.css";
import "./AdminPromotionsPage.css";

type FilterTab = "ALL" | PromotionStatus;

const FILTERS: { key: FilterTab; label: string }[] = [
  { key: "ALL", label: "All" },
  { key: "PENDING_PAYMENT", label: "Pending Payment" },
  { key: "PENDING_APPROVAL", label: "Pending Approval" },
  { key: "ACTIVE", label: "Active" },
  { key: "EXPIRED", label: "Expired" },
  { key: "CANCELLED", label: "Cancelled" },
];

const BANNER_PLACEMENTS = new Set(["HOME_BANNER", "CATEGORY_BANNER"]);

export function AdminPromotionsPage() {
  const { showToast } = useToast();
  const [promotions, setPromotions] = useState<PromotionResponse[]>([]);
  const [payments, setPayments] = useState<PaymentSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<FilterTab>("ALL");
  const [pendingCancel, setPendingCancel] = useState<PromotionResponse | null>(null);
  const [dialogLoading, setDialogLoading] = useState(false);
  const [approvingId, setApprovingId] = useState<number | null>(null);
  const [verifyingPaymentId, setVerifyingPaymentId] = useState<number | null>(null);

  // "New Promotion" form: Customer -> Slot -> Plan -> dynamic fields (ad, or banner image).
  const [slots, setSlots] = useState<PromotionSlotResponse[]>([]);
  const [allPlans, setAllPlans] = useState<PromotionPlanResponse[]>([]);
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [customerAds, setCustomerAds] = useState<AdResponse[]>([]);
  const [adsLoading, setAdsLoading] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [customerId, setCustomerId] = useState("");
  const [slotId, setSlotId] = useState("");
  const [planId, setPlanId] = useState("");
  const [adId, setAdId] = useState("");
  const [bannerFile, setBannerFile] = useState<File | null>(null);
  const [targetUrl, setTargetUrl] = useState("");
  const [paymentWaived, setPaymentWaived] = useState(false);

  const load = () => {
    setLoading(true);
    setError(null);
    return Promise.all([adminPromotionApi.listPromotions(), adminPaymentApi.listPayments()])
      .then(([promotionList, paymentList]) => {
        setPromotions(promotionList);
        setPayments(paymentList);
      })
      .catch((err) => setError(getApiErrorMessage(err, "Could not load promotions.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const visible = tab === "ALL" ? promotions : promotions.filter((p) => p.status === tab);

  const paymentForPromotion = (promotionId: number) => payments.find((p) => p.promotionId === promotionId);

  const confirmCancel = async () => {
    if (!pendingCancel) return;
    setDialogLoading(true);
    try {
      const updated = await adminPromotionApi.cancelPromotionAsAdmin(pendingCancel.id);
      setPromotions((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      showToast(pendingCancel.status === "PENDING_APPROVAL" ? "Promotion rejected." : "Promotion cancelled.");
      setPendingCancel(null);
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not cancel this promotion."), "error");
    } finally {
      setDialogLoading(false);
    }
  };

  const approve = async (promotion: PromotionResponse) => {
    setApprovingId(promotion.id);
    try {
      const updated = await adminPromotionApi.approvePromotion(promotion.id);
      setPromotions((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      showToast("Promotion approved and activated.");
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not approve this promotion."), "error");
    } finally {
      setApprovingId(null);
    }
  };

  // Verifying a payment (with or without proof) here reuses the same review modal as the
  // Admin Payments page. A verified payment may activate the promotion or move it to
  // PENDING_APPROVAL, so reload rather than patching just the payment locally.
  const handlePaymentVerified = (updated: PaymentResponse) => {
    showToast(
      updated.status === "APPROVED" ? "Payment approved and promotion activated." : `Payment ${updated.status.toLowerCase()}.`,
    );
    load();
  };

  const resetForm = () => {
    setCustomerId("");
    setSlotId("");
    setPlanId("");
    setAdId("");
    setBannerFile(null);
    setTargetUrl("");
    setPaymentWaived(false);
    setCustomerAds([]);
    setFormError(null);
  };

  const openForm = async () => {
    resetForm();
    setFormOpen(true);
    try {
      const [slotList, planList, customerList] = await Promise.all([
        adminPromotionApi.listPromotionSlots(),
        adminPromotionApi.listAllPromotionPlans(),
        listCustomers(),
      ]);
      setSlots(slotList.filter((s) => s.active));
      setAllPlans(planList);
      setCustomers(customerList);
    } catch (err) {
      setFormError(getApiErrorMessage(err, "Could not load customers, slots and plans."));
    }
  };

  useEffect(() => {
    if (!customerId) {
      setCustomerAds([]);
      return;
    }
    let cancelled = false;
    setAdsLoading(true);
    listCustomerActiveAds(customerId)
      .then((ads) => {
        if (!cancelled) setCustomerAds(ads);
      })
      .catch(() => {
        if (!cancelled) setCustomerAds([]);
      })
      .finally(() => {
        if (!cancelled) setAdsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [customerId]);

  const selectedSlot = useMemo(() => slots.find((s) => String(s.id) === slotId) ?? null, [slots, slotId]);
  const isBannerSlot = selectedSlot ? BANNER_PLACEMENTS.has(selectedSlot.placementType) : false;
  const plansForSlot = useMemo(
    () => allPlans.filter((p) => String(p.slotId) === slotId && p.active),
    [allPlans, slotId],
  );
  const selectedPlan = useMemo(() => allPlans.find((p) => String(p.id) === planId) ?? null, [allPlans, planId]);

  const handleSlotChange = (value: string) => {
    setSlotId(value);
    setPlanId("");
    setAdId("");
    setBannerFile(null);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);

    if (!customerId || !slotId || !planId) {
      setFormError("Please choose a customer, a promotion slot, and a plan.");
      return;
    }
    if (isBannerSlot && !bannerFile) {
      setFormError("Please choose a banner image.");
      return;
    }
    if (!isBannerSlot && !adId) {
      setFormError("Please choose an ad to promote.");
      return;
    }

    setSubmitting(true);
    try {
      let bannerMediaId: number | undefined;
      if (isBannerSlot && bannerFile) {
        const media = await adminPromotionApi.uploadBannerMedia(bannerFile);
        bannerMediaId = media.id;
      }

      const created = await adminPromotionApi.createPromotion({
        customerId: Number(customerId),
        promotionPlanId: Number(planId),
        adId: isBannerSlot ? undefined : Number(adId),
        bannerMediaId,
        targetUrl: isBannerSlot ? targetUrl.trim() || undefined : undefined,
        paymentWaived,
      });
      setPromotions((prev) => [created, ...prev]);
      showToast(
        created.status === "ACTIVE"
          ? "Promotion created and activated."
          : created.status === "PENDING_APPROVAL"
            ? "Promotion created, pending approval."
            : "Promotion created, pending payment.",
      );
      setFormOpen(false);
    } catch (err) {
      setFormError(getApiErrorMessage(err, "Could not create this promotion."));
    } finally {
      setSubmitting(false);
    }
  };

  const paymentLabel = (promotion: PromotionResponse) => {
    if (promotion.paymentWaived) {
      return <span className="admin-promotions-page__payment-label admin-promotions-page__payment-label--waived">Waived</span>;
    }
    if (!promotion.paymentRequired) {
      return <span className="admin-promotions-page__payment-label">Not required</span>;
    }
    const payment = paymentForPromotion(promotion.id);
    return payment ? <PaymentStatusBadge status={payment.status} /> : <PaymentStatusBadge status="PENDING" />;
  };

  return (
    <div className="admin-promotions-page">
      <AdminPageHeader
        title="Promotions"
        subtitle="Create a promotion of any placement type on behalf of a customer, or manage existing ones."
        action={
          <button type="button" className="btn btn-primary" onClick={openForm}>
            <FaPlus aria-hidden="true" /> New Promotion
          </button>
        }
      />

      {!loading && !error && promotions.length > 0 && (
        <div className="admin-promotions-page__tabs">
          {FILTERS.map(({ key, label }) => (
            <button
              key={key}
              type="button"
              className={`admin-promotions-page__tab ${tab === key ? "admin-promotions-page__tab--active" : ""}`}
              onClick={() => setTab(key)}
            >
              {label} ({key === "ALL" ? promotions.length : promotions.filter((p) => p.status === key).length})
            </button>
          ))}
        </div>
      )}

      {loading && <LoadingState label="Loading promotions…" />}

      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && promotions.length === 0 && <EmptyState title="No promotions yet." />}

      {!loading && !error && promotions.length > 0 && visible.length === 0 && (
        <EmptyState title="No promotions in this status." />
      )}

      {!loading && !error && visible.length > 0 && (
        <div className="admin-promotions-page__table-wrap">
          <table className="admin-promotions-page__table">
            <thead>
              <tr>
                <th>Customer</th>
                <th>Ad / Banner</th>
                <th>Slot</th>
                <th>Plan</th>
                <th>Price</th>
                <th>Payment</th>
                <th>Status</th>
                <th>Starts</th>
                <th>Ends</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visible.map((promotion) => (
                <tr key={promotion.id}>
                  <td>{promotion.customerDisplayName}</td>
                  <td>
                    {promotion.kind === "BANNER_PROMOTION" || !promotion.adId ? (
                      <span>Banner ({promotion.slotCode})</span>
                    ) : (
                      <Link to={`/ads/${promotion.adId}`}>{promotion.adTitle}</Link>
                    )}
                  </td>
                  <td>{promotion.slotCode}</td>
                  <td>{promotion.promotionPlanName}</td>
                  <td>{formatPrice(promotion.price)}</td>
                  <td>{paymentLabel(promotion)}</td>
                  <td>
                    <PromotionStatusBadge status={promotion.status} />
                  </td>
                  <td>{formatDate(promotion.startsAt)}</td>
                  <td>{formatDate(promotion.endsAt)}</td>
                  <td className="admin-promotions-page__actions">
                    {promotion.status === "PENDING_PAYMENT" &&
                      (() => {
                        const payment = paymentForPromotion(promotion.id);
                        if (!payment || (payment.status !== "PENDING" && payment.status !== "SUBMITTED")) return null;
                        return (
                          <button
                            type="button"
                            className="btn btn-primary"
                            onClick={() => setVerifyingPaymentId(payment.id)}
                          >
                            Verify Payment
                          </button>
                        );
                      })()}
                    {promotion.status === "PENDING_APPROVAL" && (
                      <button
                        type="button"
                        className="btn btn-primary"
                        disabled={approvingId === promotion.id}
                        onClick={() => approve(promotion)}
                      >
                        {approvingId === promotion.id ? "Approving…" : "Approve"}
                      </button>
                    )}
                    {(promotion.status === "PENDING_PAYMENT" ||
                      promotion.status === "PENDING_APPROVAL" ||
                      promotion.status === "ACTIVE") && (
                      <button type="button" className="btn btn-outline" onClick={() => setPendingCancel(promotion)}>
                        {promotion.status === "PENDING_APPROVAL" ? "Reject" : "Cancel"}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <AdminPaymentReviewModal
        paymentId={verifyingPaymentId}
        onClose={() => setVerifyingPaymentId(null)}
        onChanged={handlePaymentVerified}
      />

      <ConfirmDialog
        open={pendingCancel !== null}
        title={pendingCancel?.status === "PENDING_APPROVAL" ? "Reject this promotion?" : "Cancel this promotion?"}
        message={`"${pendingCancel?.adTitle ?? `Banner (${pendingCancel?.slotCode})`}" (${pendingCancel?.customerDisplayName}) will no longer be promoted.`}
        confirmLabel={pendingCancel?.status === "PENDING_APPROVAL" ? "Reject Promotion" : "Cancel Promotion"}
        danger
        loading={dialogLoading}
        onConfirm={confirmCancel}
        onCancel={() => setPendingCancel(null)}
      />

      <Modal open={formOpen} title="New Promotion" onClose={() => setFormOpen(false)}>
        <form className="admin-form" onSubmit={handleSubmit} noValidate>
          {formError && (
            <p className="admin-form__error" role="alert">
              {formError}
            </p>
          )}

          <div className="admin-form__field">
            <label htmlFor="promo-customer">Customer</label>
            <select
              id="promo-customer"
              value={customerId}
              onChange={(e) => {
                setCustomerId(e.target.value);
                setAdId("");
              }}
              required
            >
              <option value="" disabled>
                Select a customer
              </option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.displayName} ({c.email})
                </option>
              ))}
            </select>
          </div>

          <div className="admin-form__field">
            <label htmlFor="promo-slot">Promotion Slot</label>
            <select id="promo-slot" value={slotId} onChange={(e) => handleSlotChange(e.target.value)} required>
              <option value="" disabled>
                Select a slot
              </option>
              {slots.map((slot) => (
                <option key={slot.id} value={slot.id}>
                  {slot.name}
                  {slot.categoryName ? ` · ${slot.categoryName}` : ""}
                </option>
              ))}
            </select>
          </div>

          {slotId && (
            <div className="admin-form__field">
              <label htmlFor="promo-plan">Promotion Plan</label>
              {plansForSlot.length === 0 ? (
                <p className="admin-form__hint">No active plans for this slot yet.</p>
              ) : (
                <select id="promo-plan" value={planId} onChange={(e) => setPlanId(e.target.value)} required>
                  <option value="" disabled>
                    Select a plan
                  </option>
                  {plansForSlot.map((plan) => (
                    <option key={plan.id} value={plan.id}>
                      {plan.name} — {plan.durationDays}d — {formatPrice(plan.price)}
                      {!plan.paymentRequired ? " (free)" : ""}
                    </option>
                  ))}
                </select>
              )}
            </div>
          )}

          {planId && isBannerSlot && (
            <>
              <div className="admin-form__field">
                <label htmlFor="promo-banner-file">Banner Image</label>
                <input
                  id="promo-banner-file"
                  type="file"
                  accept="image/*"
                  onChange={(e) => setBannerFile(e.target.files?.[0] ?? null)}
                  required
                />
              </div>
              <div className="admin-form__field">
                <label htmlFor="promo-target-url">Target URL (optional)</label>
                <input
                  id="promo-target-url"
                  type="url"
                  value={targetUrl}
                  onChange={(e) => setTargetUrl(e.target.value)}
                  placeholder="https://…"
                />
              </div>
            </>
          )}

          {planId && !isBannerSlot && customerId && (
            <div className="admin-form__field">
              <label htmlFor="promo-ad">Ad</label>
              {adsLoading ? (
                <p className="admin-form__hint">Loading this customer's active ads…</p>
              ) : customerAds.length === 0 ? (
                <p className="admin-form__hint">This customer has no active ads.</p>
              ) : (
                <select id="promo-ad" value={adId} onChange={(e) => setAdId(e.target.value)} required>
                  <option value="" disabled>
                    Select an ad
                  </option>
                  {customerAds.map((ad) => (
                    <option key={ad.id} value={ad.id}>
                      {ad.title} ({ad.category})
                    </option>
                  ))}
                </select>
              )}
              <p className="admin-form__hint">Only this customer's active ads are shown; the backend still enforces category compatibility.</p>
            </div>
          )}

          {planId && (
            <div className="admin-form__field">
              <label>Start</label>
              <p className="admin-form__hint">Now — future-dated scheduling isn't supported yet.</p>
            </div>
          )}

          {selectedPlan?.paymentRequired && (
            <div className="admin-form__field">
              <label htmlFor="promo-waive">
                <input
                  id="promo-waive"
                  type="checkbox"
                  checked={paymentWaived}
                  onChange={(e) => setPaymentWaived(e.target.checked)}
                />
                Waive payment (admin complimentary promotion)
              </label>
              <p className="admin-form__hint">
                Skips the bank-transfer step for this one promotion. The plan's price is still recorded for the
                audit trail; the plan itself is unchanged.
              </p>
            </div>
          )}

          <div className="admin-form__actions">
            <button type="button" className="btn btn-secondary" onClick={() => setFormOpen(false)} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting || !planId}>
              {submitting ? "Creating…" : "Create Promotion"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
