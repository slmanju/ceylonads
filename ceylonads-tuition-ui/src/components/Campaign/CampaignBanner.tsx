import { useCampaign } from "../../campaign/CampaignContext";
import { useCampaignCta } from "../../campaign/useCampaignCta";
import { formatCampaignDurationLabel } from "../../utils/campaignDuration";
import { formatShortDate } from "../../utils/formatDate";
import "./CampaignBanner.css";

// Renders only when the backend campaign says so (showBanner) - campaign.headline/ctaLabel are
// never hardcoded here, see tuition CLAUDE.md "Promotions". The one addition is an eyebrow +
// "until" clause computed from the campaign's own startsAt/endsAt (never a hardcoded "3 months"):
// this slim, site-wide banner is the customer's first touchpoint with the launch offer, so it
// needs to make the *campaign* window ("free until Dec 10") visually distinct from each 30-day
// promotion's own duration (shown per-card on PricingPage/PromoteClassPage) without repeating the
// full campaign.message body text here - see CampaignModal for the fuller version of that copy.
export function CampaignBanner() {
  const { campaign } = useCampaign();
  const handleCta = useCampaignCta();

  if (!campaign || !campaign.showBanner) {
    return null;
  }

  const durationLabel = formatCampaignDurationLabel(campaign.startsAt, campaign.endsAt);

  return (
    <div className="campaign-banner">
      <div className="container campaign-banner__inner">
        <p className="campaign-banner__text">
          {durationLabel && <span className="campaign-banner__eyebrow">{durationLabel} Free Launch —</span>}
          <strong className="campaign-banner__headline">{campaign.headline}</strong>
          {campaign.endsAt && (
            <span className="campaign-banner__until">until {formatShortDate(campaign.endsAt)}</span>
          )}
        </p>
        <button type="button" className="campaign-banner__cta" onClick={handleCta}>
          {campaign.ctaLabel}
        </button>
      </div>
    </div>
  );
}
