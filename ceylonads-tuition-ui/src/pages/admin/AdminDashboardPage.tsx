import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getDashboardSummary } from "../../api/adminTuitionApi";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { getApiErrorMessage } from "../../utils/apiError";
import type { TuitionAdminDashboardSummary } from "../../types/api";
import "./AdminDashboardPage.css";

export function AdminDashboardPage() {
  const [summary, setSummary] = useState<TuitionAdminDashboardSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    setError(null);
    getDashboardSummary()
      .then(setSummary)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load the dashboard.")))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  if (loading) return <LoadingState label="Loading dashboard…" />;
  if (error || !summary) return <ErrorState message={error ?? undefined} onRetry={load} />;

  return (
    <div className="tuition-admin-dashboard">
      <h1>ezClass Admin Dashboard</h1>

      <div className="tuition-admin-dashboard__cards">
        <Link to="/admin/tuition/pending" className="tuition-admin-stat-card">
          <span className="tuition-admin-stat-card__value">{summary.pendingClasses}</span>
          <span className="tuition-admin-stat-card__label">Pending Classes</span>
        </Link>

        <div className="tuition-admin-stat-card">
          <span className="tuition-admin-stat-card__value">{summary.activeClasses}</span>
          <span className="tuition-admin-stat-card__label">Active Classes</span>
        </div>

        <div className="tuition-admin-stat-card">
          <span className="tuition-admin-stat-card__value">{summary.expiredClasses}</span>
          <span className="tuition-admin-stat-card__label">Expired Classes</span>
        </div>

        <Link to="/admin/tuition/promotions" className="tuition-admin-stat-card">
          <span className="tuition-admin-stat-card__value">{summary.pendingPromotions}</span>
          <span className="tuition-admin-stat-card__label">Pending Promotions</span>
        </Link>

        <Link to="/admin/tuition/promotions" className="tuition-admin-stat-card">
          <span className="tuition-admin-stat-card__value">{summary.activePromotions}</span>
          <span className="tuition-admin-stat-card__label">Active Promotions</span>
        </Link>

        <Link to="/admin/tuition/promotion-plans" className="tuition-admin-stat-card">
          <span className="tuition-admin-stat-card__value">{summary.currentPromotionPlans}</span>
          <span className="tuition-admin-stat-card__label">Current Promotion Plans</span>
        </Link>

        <Link to="/admin/tuition/campaigns" className="tuition-admin-stat-card">
          <span className="tuition-admin-stat-card__value">{summary.currentCampaigns}</span>
          <span className="tuition-admin-stat-card__label">Current Campaigns</span>
        </Link>

        <Link to="/admin/tuition/suggestions" className="tuition-admin-stat-card">
          <span className="tuition-admin-stat-card__value">{summary.newSuggestions}</span>
          <span className="tuition-admin-stat-card__label">New Suggestions</span>
        </Link>
      </div>
    </div>
  );
}
