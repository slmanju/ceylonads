// Conservative JSON-LD builders - only fields the app can actually back with real, visible data.
// Deliberately no ratings/reviews/availability/offers anywhere (see CLAUDE.md "Do not fabricate").
import { SITE_NAME, SITE_URL, absoluteUrl } from "./seo";

export function organizationJsonLd() {
  return {
    "@context": "https://schema.org",
    "@type": "Organization",
    name: SITE_NAME,
    url: SITE_URL,
  };
}

export function websiteJsonLd() {
  return {
    "@context": "https://schema.org",
    "@type": "WebSite",
    name: SITE_NAME,
    url: SITE_URL,
  };
}

export interface BreadcrumbItem {
  name: string;
  path: string;
}

export function breadcrumbListJsonLd(items: BreadcrumbItem[]) {
  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: items.map((item, index) => ({
      "@type": "ListItem",
      position: index + 1,
      name: item.name,
      item: absoluteUrl(item.path),
    })),
  };
}

// A Course is the closest accurate schema.org type for a tuition class listing without implying
// something the data doesn't support (no CourseInstance/Offer - no real schedule or price-as-offer
// data is exposed on the public detail response today).
export function courseJsonLd(opts: { title: string; description: string; canonicalPath: string }) {
  return {
    "@context": "https://schema.org",
    "@type": "Course",
    name: opts.title,
    description: opts.description,
    url: absoluteUrl(opts.canonicalPath),
    provider: {
      "@type": "Organization",
      name: SITE_NAME,
      sameAs: SITE_URL,
    },
  };
}
