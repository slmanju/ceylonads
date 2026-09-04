export const SITE_NAME = "ezClass";
export const DEFAULT_DESCRIPTION =
  "Find tuition classes, tutors and online courses across Sri Lanka on ezClass.";

// Production origin ezClass's own frontend is served from - needed to emit absolute canonical/OG
// URLs, since relative URLs are not valid there. Falls back to the local dev server origin so
// canonical/OG tags are still well-formed (if not publicly reachable) outside production builds.
export const SITE_URL = ((import.meta.env.VITE_SITE_URL as string | undefined) ?? "http://localhost:5174").replace(
  /\/$/,
  "",
);

export function absoluteUrl(pathWithQuery: string): string {
  return `${SITE_URL}${pathWithQuery.startsWith("/") ? "" : "/"}${pathWithQuery}`;
}

// Branded fallback for Open Graph/Twitter cards when a page has no image of its own (e.g. a class
// with no photos yet) - the existing hero image, not a fabricated dedicated og-image asset.
export const DEFAULT_OG_IMAGE = absoluteUrl("/images/tuition-hero.png");

export function truncateDescription(text: string, maxLength = 160): string {
  const collapsed = text.replace(/\s+/g, " ").trim();
  if (collapsed.length <= maxLength) return collapsed;
  return `${collapsed.slice(0, maxLength - 1).trimEnd()}…`;
}
