export function formatRelativeDate(isoDate: string | null): string {
  if (!isoDate) return "";

  const date = new Date(isoDate);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays <= 0) return "Today";
  if (diffDays === 1) return "Yesterday";
  if (diffDays < 7) return `${diffDays} days ago`;

  return date.toLocaleDateString("en-LK", { year: "numeric", month: "short", day: "numeric" });
}

export function formatFullDate(isoDate: string | null): string {
  if (!isoDate) return "";
  return new Date(isoDate).toLocaleDateString("en-LK", { year: "numeric", month: "short", day: "numeric" });
}

// Tuition-only listing expiry label (see AdResponse.expiresAt) - a shared helper so "Expires in N
// days"/"Expired N days ago" date math isn't duplicated across My Classes and any other place that
// shows a class's expiry. Returns null when there's nothing to show (no expiresAt, e.g. a
// MAIN_SITE/BOARDING ad or a Tuition listing still pending its first approval).
export function formatExpiryLabel(expiresAt: string | null): string | null {
  if (!expiresAt) return null;

  const msPerDay = 1000 * 60 * 60 * 24;
  const diffDays = Math.ceil((new Date(expiresAt).getTime() - Date.now()) / msPerDay);

  if (diffDays < 0) return `Expired ${formatFullDate(expiresAt)}`;
  if (diffDays === 0) return "Expires today";
  if (diffDays === 1) return "Expires tomorrow";
  return `Expires in ${diffDays} days`;
}
