import type { PromotionCampaignResponse } from "../types/api";

// Presentation-only classification derived from fields the backend already returns
// (active/startsAt/endsAt) - no extra data fetched or filtered here, the channel scoping itself
// already happened server-side (see AdminTuitionPromotionCampaignController).
export type CampaignLifecycleStatus = "CURRENT" | "SCHEDULED" | "ENDED" | "CLOSED";

export function classifyCampaign(campaign: PromotionCampaignResponse, now: Date = new Date()): CampaignLifecycleStatus {
  if (!campaign.active) return "CLOSED";
  const starts = new Date(campaign.startsAt).getTime();
  const ends = new Date(campaign.endsAt).getTime();
  const nowMs = now.getTime();
  if (nowMs < starts) return "SCHEDULED";
  if (nowMs >= ends) return "ENDED";
  return "CURRENT";
}

// Closed and Ended campaigns get restricted (read-only) editing - an admin shouldn't be able to
// accidentally reactivate/reshape an obsolete campaign's pricing semantics. Current/Scheduled
// campaigns remain fully editable.
export function isEditRestricted(status: CampaignLifecycleStatus): boolean {
  return status === "CLOSED" || status === "ENDED";
}

const STATUS_LABELS: Record<CampaignLifecycleStatus, string> = {
  CURRENT: "Current",
  SCHEDULED: "Scheduled",
  ENDED: "Ended",
  CLOSED: "Closed",
};

export function campaignStatusLabel(status: CampaignLifecycleStatus): string {
  return STATUS_LABELS[status];
}
