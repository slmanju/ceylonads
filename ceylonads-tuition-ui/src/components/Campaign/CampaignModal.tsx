import { useEffect, useState } from "react";
import { FaTimes } from "react-icons/fa";
import { useCampaign } from "../../campaign/CampaignContext";
import { useCampaignCta } from "../../campaign/useCampaignCta";
import { dismissCampaign, isCampaignDismissed } from "../../campaign/campaignDismissal";
import { CURRENT_PROMOTION_PLAN_DURATION_DAYS, formatCampaignDurationLabel } from "../../utils/campaignDuration";
import { formatFullDate } from "../../utils/formatDate";
import "./CampaignModal.css";

// Renders only when the backend campaign says so (showModal) and the visitor hasn't already
// dismissed THIS campaign (by code, see campaignDismissal.ts) - name/headline/message/ctaLabel are
// never hardcoded here. The two lines below them ARE added here: they exist specifically to keep a
// customer from confusing "the free launch campaign runs for ~3 months" with "each promotion you
// buy runs for 30 days" - see PromotionPlanCard, which draws the same distinction on every plan
// card.
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

  const durationLabel = formatCampaignDurationLabel(campaign.startsAt, campaign.endsAt);

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
        {campaign.endsAt && (
          <p className="campaign-modal__window">
            Offer valid until {formatFullDate(campaign.endsAt)}
            {durationLabel && ` (${durationLabel} launch period)`}.
          </p>
        )}
        <p className="campaign-modal__plan-duration">
          Each promotion you choose runs for {CURRENT_PROMOTION_PLAN_DURATION_DAYS} days.
        </p>
        <button type="button" className="campaign-modal__cta" onClick={handleCtaClick}>
          {campaign.ctaLabel}
        </button>
      </div>
    </div>
  );
}
