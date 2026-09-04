import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { activateCampaign, deactivateCampaign, listCampaigns } from "../../api/adminTuitionPromotionApi";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { formatFullDate } from "../../utils/formatDate";
import { getApiErrorMessage } from "../../utils/apiError";
import { campaignStatusLabel, classifyCampaign, type CampaignLifecycleStatus } from "../../utils/campaignStatus";
import type { PromotionCampaignResponse } from "../../types/api";
import "./AdminCampaignsPage.css";

function pricingLabel(c: PromotionCampaignResponse): string {
  return c.pricingType === "FIXED_PRICE" ? `Rs. ${c.fixedPrice}` : `${c.discountPercent}% off`;
}

// Current first, then Scheduled, then recently-Ended - Closed/historical campaigns live under
// their own tab and are never sorted alongside these.
const LIVE_ORDER: Record<CampaignLifecycleStatus, number> = { CURRENT: 0, SCHEDULED: 1, ENDED: 2, CLOSED: 3 };

export function AdminCampaignsPage() {
  const [campaigns, setCampaigns] = useState<PromotionCampaignResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [showHistorical, setShowHistorical] = useState(false);

  const load = () => {
    setLoading(true);
    setError(null);
    listCampaigns()
      .then(setCampaigns)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load campaigns.")))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const toggleActive = async (campaign: PromotionCampaignResponse) => {
    setBusyId(campaign.id);
    setError(null);
    try {
      campaign.active ? await deactivateCampaign(campaign.id) : await activateCampaign(campaign.id);
      load();
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not update this campaign."));
      setBusyId(null);
    }
  };

  if (loading) return <LoadingState label="Loading campaigns…" />;
  if (error && !campaigns) return <ErrorState message={error} onRetry={load} />;

  const classified = (campaigns ?? []).map((c) => ({ campaign: c, status: classifyCampaign(c) }));
  const live = classified
    .filter((c) => c.status !== "CLOSED")
    .sort((a, b) => LIVE_ORDER[a.status] - LIVE_ORDER[b.status]);
  const closed = classified.filter((c) => c.status === "CLOSED");
  const visible = showHistorical ? closed : live;

  return (
    <div className="tuition-admin-campaigns">
      <div className="tuition-admin-campaigns__header">
        <h1>Promotion Campaigns</h1>
        <Link to="/admin/tuition/campaigns/new" className="btn btn-primary">
          New Campaign
        </Link>
      </div>

      <div className="tuition-admin-campaigns__tabs">
        <button
          type="button"
          className={`tuition-admin-campaigns__tab ${!showHistorical ? "tuition-admin-campaigns__tab--active" : ""}`}
          onClick={() => setShowHistorical(false)}
        >
          Current &amp; Scheduled ({live.length})
        </button>
        <button
          type="button"
          className={`tuition-admin-campaigns__tab ${showHistorical ? "tuition-admin-campaigns__tab--active" : ""}`}
          onClick={() => setShowHistorical(true)}
        >
          Closed / Historical ({closed.length})
        </button>
      </div>

      {showHistorical && (
        <p className="tuition-admin-campaigns__hint">
          Closed campaigns, kept for audit. View-only here — reactivating an obsolete campaign with outdated pricing
          isn't offered from this tab.
        </p>
      )}

      {error && (
        <p className="tuition-admin-campaigns__error" role="alert">
          {error}
        </p>
      )}

      {visible.length === 0 ? (
        <EmptyState
          title={showHistorical ? "Nothing closed yet" : "No current or scheduled campaigns"}
          message={showHistorical ? "Closed campaigns will show up here." : "Create one to run a time-boxed promotion price override."}
        />
      ) : (
        <div className="tuition-admin-campaigns__table-wrap">
          <table className="tuition-admin-campaigns__table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Pricing</th>
                <th>Start</th>
                <th>End</th>
                <th>Status</th>
                <th>Plans</th>
                <th>Customer Visible</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visible.map(({ campaign: c, status }) => (
                <tr key={c.id}>
                  <td>
                    <div className="tuition-admin-campaigns__name">{c.name}</div>
                    <div className="tuition-admin-campaigns__code">{c.code}</div>
                  </td>
                  <td>{pricingLabel(c)}</td>
                  <td>{formatFullDate(c.startsAt)}</td>
                  <td>{formatFullDate(c.endsAt)}</td>
                  <td>
                    <span className={`tuition-admin-campaigns__status tuition-admin-campaigns__status--${status.toLowerCase()}`}>
                      {campaignStatusLabel(status)}
                    </span>
                  </td>
                  <td>{c.planIds.length}</td>
                  <td>{c.customerVisible ? "Yes" : "No"}</td>
                  <td className="tuition-admin-campaigns__actions">
                    {showHistorical ? (
                      <span className="tuition-admin-campaigns__view-only">View only</span>
                    ) : (
                      <>
                        <Link to={`/admin/tuition/campaigns/${c.id}/edit`} className="btn btn-outline">
                          View/Edit
                        </Link>
                        <button
                          type="button"
                          className="btn btn-outline"
                          disabled={busyId === c.id}
                          onClick={() => toggleActive(c)}
                        >
                          {c.active ? "Close" : "Activate"}
                        </button>
                      </>
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
