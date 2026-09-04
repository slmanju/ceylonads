import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  createPromotionPlan,
  getPromotionPlan,
  listPlanSlots,
  updatePromotionPlan,
} from "../../api/adminTuitionPromotionApi";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { getApiErrorMessage } from "../../utils/apiError";
import type { PromotionPlanResponse, PromotionSlotResponse } from "../../types/api";
import "./AdminPromotionPlanFormPage.css";

export function AdminPromotionPlanFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [loading, setLoading] = useState(isEdit);
  const [slots, setSlots] = useState<PromotionSlotResponse[]>([]);
  const [existing, setExisting] = useState<PromotionPlanResponse | null>(null);

  const [code, setCode] = useState("");
  const [slotId, setSlotId] = useState<number | "">("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [durationDays, setDurationDays] = useState(30);
  const [price, setPrice] = useState(0);
  const [paymentRequired, setPaymentRequired] = useState(true);
  const [approvalRequired, setApprovalRequired] = useState(true);
  const [active, setActive] = useState(true);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isEdit) {
      listPlanSlots().then(setSlots).catch(() => setSlots([]));
      return;
    }
    setLoading(true);
    Promise.all([id ? getPromotionPlan(id) : Promise.resolve(null), listPlanSlots()])
      .then(([plan, slotList]) => {
        setSlots(slotList);
        if (plan) {
          setExisting(plan);
          setCode(plan.code);
          setSlotId(plan.slotId);
          setName(plan.name);
          setDescription(plan.description);
          setDurationDays(plan.durationDays);
          setPrice(plan.price);
          setPaymentRequired(plan.paymentRequired);
          setApprovalRequired(plan.approvalRequired);
          setActive(plan.active);
        }
      })
      .catch((err) => setError(getApiErrorMessage(err, "Could not load this plan.")))
      .finally(() => setLoading(false));
  }, [id, isEdit]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!isEdit && !slotId) {
      setError("Please select a placement.");
      return;
    }
    if (!name.trim() || !description.trim()) {
      setError("Please fill in name and description.");
      return;
    }

    setSubmitting(true);
    try {
      if (isEdit && id) {
        await updatePromotionPlan(id, {
          name: name.trim(),
          description: description.trim(),
          price,
          durationDays,
          active,
          paymentRequired,
          approvalRequired,
        });
      } else {
        await createPromotionPlan({
          code: code.trim(),
          name: name.trim(),
          description: description.trim(),
          slotId: slotId as number,
          durationDays,
          price,
          paymentRequired,
          approvalRequired,
        });
      }
      navigate("/admin/tuition/promotion-plans");
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not save this plan."));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingState label="Loading…" />;

  return (
    <div className="tuition-admin-plan-form">
      <Link to="/admin/tuition/promotion-plans" className="tuition-admin-plan-form__back">
        ← Back to Promotion Plans
      </Link>
      <h1>{isEdit ? "Edit Promotion Plan" : "New Promotion Plan"}</h1>

      {error && (
        <p className="tuition-admin-plan-form__error" role="alert">
          {error}
        </p>
      )}

      <form onSubmit={handleSubmit} className="tuition-admin-plan-form__form">
        <div className="tuition-admin-plan-form__field">
          <label htmlFor="plan-code">Code</label>
          {isEdit ? (
            <p className="tuition-admin-plan-form__hint">{code} (immutable after creation)</p>
          ) : (
            <input id="plan-code" type="text" value={code} onChange={(e) => setCode(e.target.value)} required />
          )}
        </div>

        <div className="tuition-admin-plan-form__field">
          <label htmlFor="plan-slot">Placement</label>
          {isEdit ? (
            <p className="tuition-admin-plan-form__hint">{existing?.slotName} (fixed)</p>
          ) : (
            <select
              id="plan-slot"
              value={slotId}
              onChange={(e) => setSlotId(e.target.value ? Number(e.target.value) : "")}
              required
            >
              <option value="">Select a placement…</option>
              {slots.map((slot) => (
                <option key={slot.id} value={slot.id}>
                  {slot.name} ({slot.code})
                </option>
              ))}
            </select>
          )}
        </div>

        <div className="tuition-admin-plan-form__field">
          <label htmlFor="plan-name">Name</label>
          <input id="plan-name" type="text" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>

        <div className="tuition-admin-plan-form__field">
          <label htmlFor="plan-description">Description</label>
          <textarea
            id="plan-description"
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
          />
        </div>

        <div className="tuition-admin-plan-form__row">
          <div className="tuition-admin-plan-form__field">
            <label htmlFor="plan-duration">Duration (days)</label>
            <input
              id="plan-duration"
              type="number"
              min={1}
              value={durationDays}
              onChange={(e) => setDurationDays(Number(e.target.value))}
              required
            />
          </div>

          <div className="tuition-admin-plan-form__field">
            <label htmlFor="plan-price">Base Price (Rs.)</label>
            <input
              id="plan-price"
              type="number"
              min={0}
              step="0.01"
              value={price}
              onChange={(e) => setPrice(Number(e.target.value))}
              required
            />
          </div>
        </div>

        <div className="tuition-admin-plan-form__checkboxes">
          <label>
            <input
              type="checkbox"
              checked={paymentRequired}
              onChange={(e) => {
                setPaymentRequired(e.target.checked);
                if (e.target.checked) setApprovalRequired(true);
              }}
            />
            Payment required
          </label>
          <label>
            <input
              type="checkbox"
              checked={approvalRequired}
              disabled={paymentRequired}
              onChange={(e) => setApprovalRequired(e.target.checked)}
            />
            Approval required
          </label>
          {isEdit && (
            <label>
              <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
              Active
            </label>
          )}
        </div>

        <div className="tuition-admin-plan-form__actions">
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? "Saving…" : isEdit ? "Save Changes" : "Create Plan"}
          </button>
        </div>
      </form>
    </div>
  );
}
