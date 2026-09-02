export function formatDate(isoDate: string | null): string {
  if (!isoDate) return "—";
  return new Date(isoDate).toLocaleDateString("en-LK", { year: "numeric", month: "short", day: "numeric" });
}

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
