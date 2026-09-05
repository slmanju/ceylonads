// Every currently active ezClass Tuition promotion plan is a 30-day product (see
// ceylonads-api's V18/V22 migrations - TUITION_SEARCH_TOP_30D, TUITION_SEARCH_BOOST_30D, etc. all
// share the "_30D" suffix and durationDays = 30). Components that already load the real plan list
// (PricingPage, PromoteClassPage, PromotionPlanCard) should read a plan's own `durationDays`
// instead - this constant only exists for the site-wide campaign banner/modal, which are shown on
// every page before any promotion plan is ever fetched. If a future plan's duration diverges,
// update this alongside it.
export const CURRENT_PROMOTION_PLAN_DURATION_DAYS = 30;

// A rough, human-readable campaign-length label ("3-month"/"6-week"/"10-day") computed from the
// campaign's own startsAt/endsAt - never a hardcoded "3 months", so if EZCLASS_LAUNCH_FREE's dates
// change (or a future campaign runs a different length), this recalculates automatically. Returns
// null when either date is missing so callers can omit the phrase entirely rather than show
// something nonsensical.
export function formatCampaignDurationLabel(startsAt: string | null, endsAt: string | null): string | null {
  if (!startsAt || !endsAt) return null;

  const days = Math.round((new Date(endsAt).getTime() - new Date(startsAt).getTime()) / (1000 * 60 * 60 * 24));
  if (days <= 0) return null;

  const months = Math.round(days / 30);
  if (months >= 1) return `${months}-month`;

  const weeks = Math.round(days / 7);
  if (weeks >= 1) return `${weeks}-week`;

  return `${days}-day`;
}
