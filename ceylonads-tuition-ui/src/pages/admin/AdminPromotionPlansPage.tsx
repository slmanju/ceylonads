import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { activatePromotionPlan, deactivatePromotionPlan, listPromotionPlans } from "../../api/adminTuitionPromotionApi";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { formatPrice } from "../../utils/formatPrice";
import { getApiErrorMessage } from "../../utils/apiError";
import type { PromotionPlanResponse, TuitionCatalogScope } from "../../types/api";
import "./AdminPromotionPlansPage.css";

const TABS: { key: TuitionCatalogScope; label: string }[] = [
  { key: "CURRENT", label: "Current Catalog" },
  { key: "HISTORICAL", label: "Historical / Retired" },
];

export function AdminPromotionPlansPage() {
  const [tab, setTab] = useState<TuitionCatalogScope>("CURRENT");
  const [plans, setPlans] = useState<PromotionPlanResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = (scope: TuitionCatalogScope) => {
    setLoading(true);
    setError(null);
    listPromotionPlans(scope)
      .then(setPlans)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load promotion plans.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => load(tab), [tab]);

  const toggleActive = async (plan: PromotionPlanResponse) => {
    setBusyId(plan.id);
    setError(null);
    try {
      plan.active ? await deactivatePromotionPlan(plan.id) : await activatePromotionPlan(plan.id);
      // Activating/closing can move a plan between tabs (current <-> historical), so re-fetch
      // this tab's list rather than patch the row in place.
      load(tab);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not update this plan."));
      setBusyId(null);
    }
  };

  const isCurrentTab = tab === "CURRENT";

  return (
    <div className="tuition-admin-plans">
      <div className="tuition-admin-plans__header">
        <h1>Promotion Plans</h1>
        <Link to="/admin/tuition/promotion-plans/new" className="btn btn-primary">
          New Promotion Plan
        </Link>
      </div>

      <div className="tuition-admin-plans__tabs">
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            className={`tuition-admin-plans__tab ${tab === t.key ? "tuition-admin-plans__tab--active" : ""}`}
            onClick={() => setTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {!isCurrentTab && (
        <p className="tuition-admin-plans__hint">
          Retired or test products, kept for audit only. View-only here — reactivate a placement by editing a
          current-catalog plan on the same slot instead.
        </p>
      )}

      {error && (
        <p className="tuition-admin-plans__error" role="alert">
          {error}
        </p>
      )}

      {loading && <LoadingState label="Loading promotion plans…" />}

      {!loading && !plans && <ErrorState message={error ?? undefined} onRetry={() => load(tab)} />}

      {!loading && plans && plans.length === 0 && (
        <EmptyState
          title={isCurrentTab ? "No current promotion plans" : "Nothing historical"}
          message={isCurrentTab ? "Create one to let tutors promote their classes." : "No retired plans on record."}
        />
      )}

      {!loading && plans && plans.length > 0 && (
        <div className="tuition-admin-plans__table-wrap">
          <table className="tuition-admin-plans__table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Slot</th>
                <th>Duration</th>
                <th>Base Price</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {plans.map((plan) => (
                <tr key={plan.id}>
                  <td>
                    <div className="tuition-admin-plans__name">{plan.name}</div>
                    <div className="tuition-admin-plans__code">{plan.code}</div>
                  </td>
                  <td>{plan.slotName}</td>
                  <td>{plan.durationDays} days</td>
                  <td>{formatPrice(plan.price)}</td>
                  <td>
                    <span className={`tuition-admin-plans__status ${plan.active ? "tuition-admin-plans__status--active" : ""}`}>
                      {plan.active ? "Active" : "Closed"}
                    </span>
                  </td>
                  <td className="tuition-admin-plans__actions">
                    {isCurrentTab ? (
                      <>
                        <Link to={`/admin/tuition/promotion-plans/${plan.id}/edit`} className="btn btn-outline">
                          View/Edit
                        </Link>
                        <button
                          type="button"
                          className="btn btn-outline"
                          disabled={busyId === plan.id}
                          onClick={() => toggleActive(plan)}
                        >
                          {plan.active ? "Close" : "Activate"}
                        </button>
                      </>
                    ) : (
                      <span className="tuition-admin-plans__view-only">View only</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
