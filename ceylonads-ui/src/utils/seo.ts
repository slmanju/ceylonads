export const SITE_NAME = "CeylonAds";
export const DEFAULT_DESCRIPTION =
  "Buy and sell vehicles, property, mobiles, tuition and services across Sri Lanka on CeylonAds.";
export const DEFAULT_OG_IMAGE = "/og/og-default.png";

export function absoluteUrl(path: string): string {
  if (/^https?:\/\//i.test(path)) return path;
  return `${window.location.origin}${path.startsWith("/") ? "" : "/"}${path}`;
}

export function truncateDescription(text: string, maxLength = 160): string {
  const collapsed = text.replace(/\s+/g, " ").trim();
  if (collapsed.length <= maxLength) return collapsed;
  return `${collapsed.slice(0, maxLength - 1).trimEnd()}…`;
}

export interface BreadcrumbItem {
  name: string;
  path?: string;
}

// Schema.org BreadcrumbList: https://schema.org/BreadcrumbList
export function buildBreadcrumbJsonLd(items: BreadcrumbItem[]) {
  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: items.map((item, index) => ({
      "@type": "ListItem",
      position: index + 1,
      name: item.name,
      ...(item.path ? { item: absoluteUrl(item.path) } : {}),
    })),
  };
}
