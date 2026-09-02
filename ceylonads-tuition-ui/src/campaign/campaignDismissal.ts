const DISMISSAL_KEY_PREFIX = "tuitionCampaignDismissed:";

// localStorage, not sessionStorage: campaigns run for weeks/months (see promotion_campaigns.ends_at),
// so a dismissal should persist across browser sessions, not just the current tab. Keyed by
// campaign code (never a fixed key) so dismissing one campaign's modal never suppresses a later,
// different campaign - see tuition CLAUDE.md "Promotions".
export function isCampaignDismissed(code: string): boolean {
  return localStorage.getItem(DISMISSAL_KEY_PREFIX + code) === "true";
}

export function dismissCampaign(code: string): void {
  localStorage.setItem(DISMISSAL_KEY_PREFIX + code, "true");
}
