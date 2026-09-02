import { useCampaign } from "../../campaign/CampaignContext";
import { useCampaignCta } from "../../campaign/useCampaignCta";
import "./CampaignBanner.css";

// Renders only when the backend campaign says so (showBanner) - no campaign-specific copy is
// hardcoded here. The same component renders any campaign's headline/message/ctaLabel unchanged,
// see tuition CLAUDE.md "Promotions".
export function CampaignBanner() {
  const { campaign } = useCampaign();
  const handleCta = useCampaignCta();

  if (!campaign || !campaign.showBanner) {
    return null;
  }

  return (
    <div className="campaign-banner">
      <div className="container campaign-banner__inner">
        <p className="campaign-banner__text">
          <strong className="campaign-banner__headline">{campaign.headline}</strong>
          <span className="campaign-banner__message">{campaign.message}</span>
        </p>
        <button type="button" className="campaign-banner__cta" onClick={handleCta}>
          {campaign.ctaLabel}
        </button>
      </div>
    </div>
  );
}
