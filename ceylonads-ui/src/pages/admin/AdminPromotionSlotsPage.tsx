import { useEffect, useState, type FormEvent } from "react";
import { FaPlus } from "react-icons/fa";
import * as adminPromotionApi from "../../api/adminPromotionApi";
import { AdminPageHeader } from "../../components/AdminPageHeader/AdminPageHeader";
import { Modal } from "../../components/Modal/Modal";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { PromotionStatusBadge } from "../../components/PromotionStatusBadge/PromotionStatusBadge";
import { useToast } from "../../components/Toast/ToastProvider";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatDate } from "../../utils/formatDate";
import { formatPrice } from "../../utils/formatPrice";
import type {
  PlacementType,
  PromotionPlanResponse,
  PromotionSlotResponse,
  PromotionSlotUsageResponse,
  SourceChannel,
} from "../../types/api";
import "./AdminForm.css";
import "./AdminPromotionSlotsPage.css";

const PLACEMENT_LABELS: Record<PlacementType, string> = {
  HOME_FEATURED: "Homepage Featured",
  HOME_BANNER: "Homepage Banner",
  CATEGORY_FEATURED: "Category Featured",
  CATEGORY_BANNER: "Category Banner",
  TOP_SEARCH: "Top Search",
};

interface FormState {
  code: string;
  name: string;
  description: string;
  placementType: PlacementType;
  categorySlug: string;
  sourceChannel: SourceChannel;
  capacity: string;
  visibleCount: string;
  displayOrder: string;
  active: boolean;
}

const EMPTY_FORM: FormState = {
  code: "",
  name: "",
  description: "",
  placementType: "HOME_FEATURED",
  categorySlug: "",
  sourceChannel: "MAIN_SITE",
  capacity: "1",
  visibleCount: "1",
  displayOrder: "0",
  active: true,
};

export function AdminPromotionSlotsPage() {
  const { showToast } = useToast();
  const [slots, setSlots] = useState<PromotionSlotResponse[]>([]);
  const [plans, setPlans] = useState<PromotionPlanResponse[]>([]);
  const [usageBySlot, setUsageBySlot] = useState<Record<number, PromotionSlotUsageResponse>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [formOpen, setFormOpen] = useState(false);
  const [editingSlot, setEditingSlot] = useState<PromotionSlotResponse | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);

  const [usage, setUsage] = useState<PromotionSlotUsageResponse | null>(null);
  const [usageLoading, setUsageLoading] = useState(false);

  const load = () => {
    setLoading(true);
    setError(null);
    return Promise.all([adminPromotionApi.listPromotionSlots(), adminPromotionApi.listAllPromotionPlans()])
      .then(async ([slotList, planList]) => {
        setSlots(slotList);
        setPlans(planList);
        const usageEntries = await Promise.all(
          slotList.map((slot) => adminPromotionApi.getPromotionSlotUsage(slot.id).then((usage) => [slot.id, usage] as const)),
        );
        setUsageBySlot(Object.fromEntries(usageEntries));
      })
      .catch((err) => setError(getApiErrorMessage(err, "Could not load promotion slots.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openCreateForm = () => {
    setEditingSlot(null);
    setForm(EMPTY_FORM);
    setFormError(null);
    setFormOpen(true);
  };

  const openEditForm = (slot: PromotionSlotResponse) => {
    setEditingSlot(slot);
    setForm({
      code: slot.code,
      name: slot.name,
      description: slot.description,
      placementType: slot.placementType,
      categorySlug: slot.categorySlug ?? "",
      sourceChannel: slot.sourceChannel,
      capacity: String(slot.capacity),
      visibleCount: String(slot.visibleCount),
      displayOrder: String(slot.displayOrder),
      active: slot.active,
    });
    setFormError(null);
    setFormOpen(true);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);

    const capacity = Number(form.capacity);
    const visibleCount = Number(form.visibleCount);
    const displayOrder = Number(form.displayOrder) || 0;

    if (!form.name.trim() || !form.description.trim() || !Number.isFinite(capacity) || capacity < 1) {
      setFormError("Please fill in all fields with valid values.");
      return;
    }
    if (!Number.isFinite(visibleCount) || visibleCount < 1) {
      setFormError("Visible at once must be at least 1.");
      return;
    }
    if (visibleCount > capacity) {
      setFormError("Visible at once cannot exceed capacity.");
      return;
    }

    setSubmitting(true);
    try {
      if (editingSlot) {
        const updated = await adminPromotionApi.updatePromotionSlot(editingSlot.id, {
          name: form.name.trim(),
          description: form.description.trim(),
          capacity,
          visibleCount,
          displayOrder,
          active: form.active,
        });
        setSlots((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));
        showToast("Promotion slot updated.");
      } else {
        if (!form.code.trim()) {
          setFormError("Code is required.");
          setSubmitting(false);
          return;
        }
        const created = await adminPromotionApi.createPromotionSlot({
          code: form.code.trim(),
          name: form.name.trim(),
          description: form.description.trim(),
          placementType: form.placementType,
          categorySlug: form.categorySlug.trim() || undefined,
          sourceChannel: form.sourceChannel,
          capacity,
          visibleCount,
          displayOrder,
        });
        setSlots((prev) => [...prev, created]);
        showToast("Promotion slot created.");
      }
      setFormOpen(false);
    } catch (err) {
      setFormError(getApiErrorMessage(err, "Could not save this promotion slot."));
    } finally {
      setSubmitting(false);
    }
  };

  const toggleActive = async (slot: PromotionSlotResponse) => {
    setBusyId(slot.id);
    try {
      const updated = slot.active
        ? await adminPromotionApi.deactivatePromotionSlot(slot.id)
        : await adminPromotionApi.activatePromotionSlot(slot.id);
      setSlots((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));
      showToast(updated.active ? "Promotion slot activated." : "Promotion slot deactivated.");
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not update this promotion slot."), "error");
    } finally {
      setBusyId(null);
    }
  };

  const openUsage = async (slot: PromotionSlotResponse) => {
    setUsage(null);
    setUsageLoading(true);
    try {
      const data = await adminPromotionApi.getPromotionSlotUsage(slot.id);
      setUsage(data);
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not load slot usage."), "error");
    } finally {
      setUsageLoading(false);
    }
  };

  const categoryScoped = form.placementType === "CATEGORY_FEATURED" || form.placementType === "CATEGORY_BANNER";

  return (
    <div className="admin-promotion-slots-page">
      <AdminPageHeader
        title="Promotion Slots"
        subtitle="Manage the physical, capacity-limited placement inventory promotion plans sell."
        action={
          <button type="button" className="btn btn-primary" onClick={openCreateForm}>
            <FaPlus aria-hidden="true" /> New Slot
          </button>
        }
      />

      {loading && <LoadingState label="Loading promotion slots…" />}

      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && slots.length === 0 && <EmptyState title="No promotion slots yet." />}

      {!loading && !error && slots.length > 0 && (
        <div className="admin-promotion-slots-page__table-wrap">
          <table className="admin-promotion-slots-page__table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Placement</th>
                <th>Category</th>
                <th>Capacity</th>
                <th>Visible</th>
                <th>Active</th>
                <th>Available</th>
                <th>Plans</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {slots.map((slot) => {
                const slotUsage = usageBySlot[slot.id];
                const slotPlans = plans.filter((p) => p.slotId === slot.id);
                return (
                <tr key={slot.id}>
                  <td>
                    <p className="admin-promotion-slots-page__name">{slot.name}</p>
                    <p className="admin-promotion-slots-page__code">{slot.code}</p>
                  </td>
                  <td>{PLACEMENT_LABELS[slot.placementType]}</td>
                  <td>{slot.categoryName ?? "—"}</td>
                  <td>{slot.capacity}</td>
                  <td>{slot.visibleCount}</td>
                  <td>{slotUsage ? slotUsage.activeCount : "—"}</td>
                  <td>{slotUsage ? slotUsage.remainingCapacity : "—"}</td>
                  <td>
                    {slotPlans.length === 0 ? (
                      <span className="admin-promotion-slots-page__no-plans">No plans yet</span>
                    ) : (
                      <ul className="admin-promotion-slots-page__plan-list">
                        {slotPlans.map((plan) => (
                          <li key={plan.id}>
                            {plan.durationDays} days — {formatPrice(plan.price)}
                            {!plan.active ? " (inactive)" : ""}
                          </li>
                        ))}
                      </ul>
                    )}
                  </td>
                  <td>
                    <span
                      className={`admin-promotion-slots-page__status ${slot.active ? "admin-promotion-slots-page__status--active" : ""}`}
                    >
                      {slot.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="admin-promotion-slots-page__actions">
                    <button type="button" className="btn btn-outline" onClick={() => openUsage(slot)}>
                      Usage
                    </button>
                    <button type="button" className="btn btn-secondary" onClick={() => openEditForm(slot)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      className="btn btn-outline"
                      disabled={busyId === slot.id}
                      onClick={() => toggleActive(slot)}
                    >
                      {slot.active ? "Deactivate" : "Activate"}
                    </button>
                  </td>
                </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <Modal open={formOpen} title={editingSlot ? "Edit Promotion Slot" : "New Promotion Slot"} onClose={() => setFormOpen(false)}>
        <form className="admin-form" onSubmit={handleSubmit} noValidate>
          {formError && (
            <p className="admin-form__error" role="alert">
              {formError}
            </p>
          )}

          {!editingSlot && (
            <div className="admin-form__field">
              <label htmlFor="slot-code">Code</label>
              <input
                id="slot-code"
                type="text"
                value={form.code}
                onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
                placeholder="e.g. VEHICLES_FEATURED"
                required
              />
            </div>
          )}

          <div className="admin-form__field">
            <label htmlFor="slot-name">Name</label>
            <input
              id="slot-name"
              type="text"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              required
            />
          </div>

          <div className="admin-form__field">
            <label htmlFor="slot-description">Description</label>
            <textarea
              id="slot-description"
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              rows={3}
              required
            />
          </div>

          {!editingSlot && (
            <div className="admin-form__field">
              <label htmlFor="slot-placement">Placement Type</label>
              <select
                id="slot-placement"
                value={form.placementType}
                onChange={(e) => setForm((f) => ({ ...f, placementType: e.target.value as PlacementType }))}
              >
                {(Object.keys(PLACEMENT_LABELS) as PlacementType[]).map((type) => (
                  <option key={type} value={type}>
                    {PLACEMENT_LABELS[type]}
                  </option>
                ))}
              </select>
              <p className="admin-form__hint">Placement can't be changed after a slot is created.</p>
            </div>
          )}
          {editingSlot && (
            <p className="admin-form__hint">
              Placement: {PLACEMENT_LABELS[editingSlot.placementType]}
              {editingSlot.categoryName ? ` · ${editingSlot.categoryName}` : ""} (fixed)
            </p>
          )}

          {!editingSlot && (
            <div className="admin-form__field">
              <label htmlFor="slot-source-channel">Channel</label>
              <select
                id="slot-source-channel"
                value={form.sourceChannel}
                onChange={(e) => setForm((f) => ({ ...f, sourceChannel: e.target.value as SourceChannel }))}
              >
                <option value="MAIN_SITE">Main Site</option>
                <option value="TUITION">Tuition</option>
                <option value="BOARDING">Boarding</option>
              </select>
              <p className="admin-form__hint">Which storefront this slot's inventory belongs to; can't be changed after a slot is created.</p>
            </div>
          )}
          {editingSlot && (
            <p className="admin-form__hint">Channel: {editingSlot.sourceChannel} (fixed)</p>
          )}

          {!editingSlot && categoryScoped && (
            <div className="admin-form__field">
              <label htmlFor="slot-category">Category Slug</label>
              <input
                id="slot-category"
                type="text"
                value={form.categorySlug}
                onChange={(e) => setForm((f) => ({ ...f, categorySlug: e.target.value }))}
                placeholder="e.g. vehicles"
                required
              />
            </div>
          )}

          <div className="admin-form__field">
            <label htmlFor="slot-capacity">Capacity</label>
            <input
              id="slot-capacity"
              type="number"
              min={1}
              value={form.capacity}
              onChange={(e) => setForm((f) => ({ ...f, capacity: e.target.value }))}
              required
            />
            <p className="admin-form__hint">Maximum active/scheduled campaigns allowed.</p>
          </div>

          <div className="admin-form__field">
            <label htmlFor="slot-visible-count">Visible at once</label>
            <input
              id="slot-visible-count"
              type="number"
              min={1}
              max={Number(form.capacity) || undefined}
              value={form.visibleCount}
              onChange={(e) => setForm((f) => ({ ...f, visibleCount: e.target.value }))}
              required
            />
            <p className="admin-form__hint">Maximum campaigns shown to a visitor at one time.</p>
          </div>

          <div className="admin-form__field">
            <label htmlFor="slot-order">Display Order</label>
            <input
              id="slot-order"
              type="number"
              value={form.displayOrder}
              onChange={(e) => setForm((f) => ({ ...f, displayOrder: e.target.value }))}
            />
          </div>

          {editingSlot && (
            <div className="admin-form__field">
              <label htmlFor="slot-active">
                <input
                  id="slot-active"
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
              {submitting ? "Saving…" : editingSlot ? "Save Changes" : "Create Slot"}
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={usage !== null || usageLoading} title={usage ? `${usage.slot.name} — Usage` : "Loading usage…"} onClose={() => setUsage(null)}>
        {usageLoading && <LoadingState label="Loading usage…" />}
        {usage && (
          <div className="admin-promotion-slots-page__usage">
            <div className="admin-promotion-slots-page__usage-stats">
              <div>
                <span>{usage.activeCount}</span>
                <p>Active</p>
              </div>
              <div>
                <span>{usage.pendingPaymentCount}</span>
                <p>Pending payment</p>
              </div>
              <div>
                <span>{usage.remainingCapacity}</span>
                <p>Remaining</p>
              </div>
              <div>
                <span>{usage.slot.capacity}</span>
                <p>Capacity</p>
              </div>
              <div>
                <span>{usage.slot.visibleCount}</span>
                <p>Visible</p>
              </div>
            </div>

            <h3 className="admin-promotion-slots-page__usage-heading">Active</h3>
            {usage.activePromotions.length === 0 ? (
              <p className="admin-form__hint">No active promotions.</p>
            ) : (
              <ul className="admin-promotion-slots-page__usage-list">
                {usage.activePromotions.map((p) => (
                  <li key={p.id}>
                    <span>{p.kind === "BANNER_PROMOTION" ? "Banner" : p.adTitle}</span>
                    <span>{p.customerDisplayName}</span>
                    <span>
                      {formatDate(p.startsAt)} – {formatDate(p.endsAt)}
                    </span>
                    <PromotionStatusBadge status={p.status} />
                  </li>
                ))}
              </ul>
            )}

            <h3 className="admin-promotion-slots-page__usage-heading">Pending Payment</h3>
            {usage.pendingPromotions.length === 0 ? (
              <p className="admin-form__hint">No promotions awaiting payment.</p>
            ) : (
              <ul className="admin-promotion-slots-page__usage-list">
                {usage.pendingPromotions.map((p) => (
                  <li key={p.id}>
                    <span>{p.kind === "BANNER_PROMOTION" ? "Banner" : p.adTitle}</span>
                    <span>{p.customerDisplayName}</span>
                    <span>Created {formatDate(p.createdAt)}</span>
                    <PromotionStatusBadge status={p.status} />
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
