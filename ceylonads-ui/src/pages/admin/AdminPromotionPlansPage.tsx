import { useEffect, useState, type FormEvent } from "react";
import { FaPlus } from "react-icons/fa";
import * as adminPromotionApi from "../../api/adminPromotionApi";
import { AdminPageHeader } from "../../components/AdminPageHeader/AdminPageHeader";
import { Modal } from "../../components/Modal/Modal";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { useToast } from "../../components/Toast/ToastProvider";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatPrice } from "../../utils/formatPrice";
import type { PromotionPlanResponse, PromotionSlotResponse } from "../../types/api";
import "./AdminForm.css";
import "./AdminPromotionPlansPage.css";

interface FormState {
  code: string;
  name: string;
  description: string;
  slotId: string;
  durationDays: string;
  price: string;
  displayOrder: string;
  active: boolean;
  paymentRequired: boolean;
  approvalRequired: boolean;
}

const EMPTY_FORM: FormState = {
  code: "",
  name: "",
  description: "",
  slotId: "",
  durationDays: "7",
  price: "",
  displayOrder: "0",
  active: true,
  paymentRequired: true,
  approvalRequired: true,
};

export function AdminPromotionPlansPage() {
  const { showToast } = useToast();
  const [plans, setPlans] = useState<PromotionPlanResponse[]>([]);
  const [slots, setSlots] = useState<PromotionSlotResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [formOpen, setFormOpen] = useState(false);
  const [editingPlan, setEditingPlan] = useState<PromotionPlanResponse | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    return Promise.all([adminPromotionApi.listAllPromotionPlans(), adminPromotionApi.listPromotionSlots()])
      .then(([planList, slotList]) => {
        setPlans(planList);
        setSlots(slotList);
      })
      .catch((err) => setError(getApiErrorMessage(err, "Could not load promotion plans.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openCreateForm = () => {
    setEditingPlan(null);
    setForm({ ...EMPTY_FORM, slotId: slots[0] ? String(slots[0].id) : "" });
    setFormError(null);
    setFormOpen(true);
  };

  const openEditForm = (plan: PromotionPlanResponse) => {
    setEditingPlan(plan);
    setForm({
      code: plan.code,
      name: plan.name,
      description: plan.description,
      slotId: String(plan.slotId),
      durationDays: String(plan.durationDays),
      price: String(plan.price),
      displayOrder: String(plan.displayOrder),
      active: plan.active,
      paymentRequired: plan.paymentRequired,
      approvalRequired: plan.approvalRequired,
    });
    setFormError(null);
    setFormOpen(true);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);

    const durationDays = Number(form.durationDays);
    const price = Number(form.price);
    const displayOrder = Number(form.displayOrder) || 0;

    if (!form.name.trim() || !form.description.trim() || !Number.isFinite(price) || !Number.isFinite(durationDays)) {
      setFormError("Please fill in all fields with valid values.");
      return;
    }
    if (form.paymentRequired && !form.approvalRequired) {
      setFormError("A plan that requires payment must also require approval.");
      return;
    }

    setSubmitting(true);
    try {
      if (editingPlan) {
        const updated = await adminPromotionApi.updatePromotionPlan(editingPlan.id, {
          name: form.name.trim(),
          description: form.description.trim(),
          price,
          durationDays,
          active: form.active,
          paymentRequired: form.paymentRequired,
          approvalRequired: form.approvalRequired,
          displayOrder,
        });
        setPlans((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
        showToast("Promotion plan updated.");
      } else {
        if (!form.code.trim()) {
          setFormError("Code is required.");
          setSubmitting(false);
          return;
        }
        if (!form.slotId) {
          setFormError("Please choose a slot.");
          setSubmitting(false);
          return;
        }
        const created = await adminPromotionApi.createPromotionPlan({
          code: form.code.trim(),
          name: form.name.trim(),
          description: form.description.trim(),
          slotId: Number(form.slotId),
          durationDays,
          price,
          paymentRequired: form.paymentRequired,
          approvalRequired: form.approvalRequired,
          displayOrder,
        });
        setPlans((prev) => [...prev, created]);
        showToast("Promotion plan created.");
      }
      setFormOpen(false);
    } catch (err) {
      setFormError(getApiErrorMessage(err, "Could not save this promotion plan."));
    } finally {
      setSubmitting(false);
    }
  };

  const toggleActive = async (plan: PromotionPlanResponse) => {
    setBusyId(plan.id);
    try {
      const updated = plan.active
        ? await adminPromotionApi.deactivatePromotionPlan(plan.id)
        : await adminPromotionApi.activatePromotionPlan(plan.id);
      setPlans((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      showToast(updated.active ? "Promotion plan activated." : "Promotion plan deactivated.");
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not update this promotion plan."), "error");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="admin-promotion-plans-page">
      <AdminPageHeader
        title="Promotion Plans"
        subtitle="Manage the paid placements customers can purchase for their ads."
        action={
          <button type="button" className="btn btn-primary" onClick={openCreateForm} disabled={slots.length === 0}>
            <FaPlus aria-hidden="true" /> New Plan
          </button>
        }
      />

      {loading && <LoadingState label="Loading promotion plans…" />}

      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && plans.length === 0 && <EmptyState title="No promotion plans yet." />}

      {!loading && !error && plans.length > 0 && (
        <div className="admin-promotion-plans-page__table-wrap">
          <table className="admin-promotion-plans-page__table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Slot</th>
                <th>Duration</th>
                <th>Price</th>
                <th>Payment / Approval</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {plans.map((plan) => (
                <tr key={plan.id}>
                  <td>
                    <p className="admin-promotion-plans-page__name">{plan.name}</p>
                    <p className="admin-promotion-plans-page__code">{plan.code}</p>
                  </td>
                  <td>
                    {plan.slotName}
                    {plan.categoryName ? ` · ${plan.categoryName}` : ""}
                  </td>
                  <td>{plan.durationDays} days</td>
                  <td>{formatPrice(plan.price)}</td>
                  <td>
                    {plan.paymentRequired ? "Payment required" : "No payment"}
                    {plan.approvalRequired ? " · Approval required" : " · Auto-activates"}
                  </td>
                  <td>
                    <span
                      className={`admin-promotion-plans-page__status ${plan.active ? "admin-promotion-plans-page__status--active" : ""}`}
                    >
                      {plan.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="admin-promotion-plans-page__actions">
                    <button type="button" className="btn btn-secondary" onClick={() => openEditForm(plan)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      className="btn btn-outline"
                      disabled={busyId === plan.id}
                      onClick={() => toggleActive(plan)}
                    >
                      {plan.active ? "Deactivate" : "Activate"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={formOpen}
        title={editingPlan ? "Edit Promotion Plan" : "New Promotion Plan"}
        onClose={() => setFormOpen(false)}
      >
        <form className="admin-form" onSubmit={handleSubmit} noValidate>
          {formError && (
            <p className="admin-form__error" role="alert">
              {formError}
            </p>
          )}

          {!editingPlan && (
            <div className="admin-form__field">
              <label htmlFor="plan-code">Code</label>
              <input
                id="plan-code"
                type="text"
                value={form.code}
                onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
                placeholder="e.g. HOME_FEATURED_7D"
                required
              />
            </div>
          )}

          <div className="admin-form__field">
            <label htmlFor="plan-name">Name</label>
            <input
              id="plan-name"
              type="text"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              required
            />
          </div>

          <div className="admin-form__field">
            <label htmlFor="plan-description">Description</label>
            <textarea
              id="plan-description"
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              rows={3}
              required
            />
          </div>

          {!editingPlan && (
            <div className="admin-form__field">
              <label htmlFor="plan-slot">Slot</label>
              <select
                id="plan-slot"
                value={form.slotId}
                onChange={(e) => setForm((f) => ({ ...f, slotId: e.target.value }))}
              >
                {slots.map((slot) => (
                  <option key={slot.id} value={slot.id}>
                    {slot.name} ({slot.code}){slot.categoryName ? ` · ${slot.categoryName}` : ""}
                  </option>
                ))}
              </select>
              <p className="admin-form__hint">
                The slot can't be changed after a plan is created. Manage slots on the Promotion Slots page.
              </p>
            </div>
          )}
          {editingPlan && (
            <p className="admin-form__hint">
              Slot: {editingPlan.slotName}
              {editingPlan.categoryName ? ` · ${editingPlan.categoryName}` : ""} (fixed)
            </p>
          )}

          <div className="admin-form__field">
            <label htmlFor="plan-duration">Duration (days)</label>
            <input
              id="plan-duration"
              type="number"
              min={1}
              value={form.durationDays}
              onChange={(e) => setForm((f) => ({ ...f, durationDays: e.target.value }))}
              required
            />
          </div>

          <div className="admin-form__field">
            <label htmlFor="plan-price">Price (Rs.)</label>
            <input
              id="plan-price"
              type="number"
              min={0}
              step="0.01"
              value={form.price}
              onChange={(e) => setForm((f) => ({ ...f, price: e.target.value }))}
              required
            />
          </div>

          <div className="admin-form__field">
            <label htmlFor="plan-payment-required">
              <input
                id="plan-payment-required"
                type="checkbox"
                checked={form.paymentRequired}
                onChange={(e) => {
                  const paymentRequired = e.target.checked;
                  setForm((f) => ({
                    ...f,
                    paymentRequired,
                    approvalRequired: paymentRequired ? true : f.approvalRequired,
                  }));
                }}
              />
              Payment Required
            </label>
            <p className="admin-form__hint">Customer must complete payment before activation.</p>
          </div>

          <div className="admin-form__field">
            <label htmlFor="plan-approval-required">
              <input
                id="plan-approval-required"
                type="checkbox"
                checked={form.approvalRequired}
                disabled={form.paymentRequired}
                onChange={(e) => setForm((f) => ({ ...f, approvalRequired: e.target.checked }))}
              />
              Approval Required
            </label>
            <p className="admin-form__hint">
              Admin approval is required before activation.
              {form.paymentRequired ? " Always required when payment is required." : ""}
            </p>
          </div>

          <div className="admin-form__field">
            <label htmlFor="plan-order">Display Order</label>
            <input
              id="plan-order"
              type="number"
              value={form.displayOrder}
              onChange={(e) => setForm((f) => ({ ...f, displayOrder: e.target.value }))}
            />
          </div>

          {editingPlan && (
            <div className="admin-form__field">
              <label htmlFor="plan-active">
                <input
                  id="plan-active"
                  type="checkbox"
                  checked={form.active}
                  onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))}
                />
                Active
              </label>
            </div>
          )}

          <div className="admin-form__actions">
            <button type="button" className="btn btn-secondary" onClick={() => setFormOpen(false)} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? "Saving…" : editingPlan ? "Save Changes" : "Create Plan"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
