import { useEffect, useState } from "react";
import { FaTimes } from "react-icons/fa";
import { useCampaign } from "../../campaign/CampaignContext";
import { useCampaignCta } from "../../campaign/useCampaignCta";
import { dismissCampaign, isCampaignDismissed } from "../../campaign/campaignDismissal";
import "./CampaignModal.css";

// Renders only when the backend campaign says so (showModal) and the visitor hasn't already
// dismissed THIS campaign (by code, see campaignDismissal.ts) - no campaign-specific copy is
// hardcoded here, and headline/message are never derived from pricing values.
export function CampaignModal() {
  const { campaign } = useCampaign();
  const handleCta = useCampaignCta();
  // Defaults to dismissed so a campaign already dismissed in a prior visit never flashes on
  // screen before the effect below checks localStorage.
  const [dismissed, setDismissed] = useState(true);

  useEffect(() => {
    if (campaign) {
      setDismissed(isCampaignDismissed(campaign.code));
    }
  }, [campaign]);

  if (!campaign || !campaign.showModal || dismissed) {
    return null;
  }

  const handleClose = () => {
    dismissCampaign(campaign.code);
    setDismissed(true);
  };

  const handleCtaClick = () => {
    dismissCampaign(campaign.code);
    setDismissed(true);
    handleCta();
  };

  return (
    <div className="campaign-modal-overlay" role="presentation" onClick={handleClose}>
      <div
        className="campaign-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="campaign-modal-headline"
        onClick={(event) => event.stopPropagation()}
      >
        <button type="button" className="campaign-modal__close" aria-label="Close" onClick={handleClose}>
          <FaTimes aria-hidden="true" />
        </button>
        <span className="campaign-modal__eyebrow">{campaign.name}</span>
        <h2 id="campaign-modal-headline" className="campaign-modal__headline">
          {campaign.headline}
        </h2>
        <p className="campaign-modal__message">{campaign.message}</p>
        <button type="button" className="campaign-modal__cta" onClick={handleCtaClick}>
          {campaign.ctaLabel}
        </button>
      </div>
    </div>
  );
}
