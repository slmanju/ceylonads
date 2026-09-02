export const SITE_NAME = "ezClass";
export const DEFAULT_DESCRIPTION =
  "Find tuition classes, tutors and online courses across Sri Lanka on ezClass.";

export function truncateDescription(text: string, maxLength = 160): string {
  const collapsed = text.replace(/\s+/g, " ").trim();
  if (collapsed.length <= maxLength) return collapsed;
  return `${collapsed.slice(0, maxLength - 1).trimEnd()}…`;
}
