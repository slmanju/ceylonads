import { useEffect } from "react";
import { DEFAULT_DESCRIPTION, DEFAULT_OG_IMAGE, SITE_NAME, absoluteUrl } from "../../utils/seo";

interface SeoProps {
  title: string;
  description?: string;
  /** Path (no origin) this page should canonicalize to. Defaults to the current path with no query string. */
  canonicalPath?: string;
  /** Marks account/admin/auth pages as non-indexable. */
  noindex?: boolean;
  ogType?: "website" | "article" | "product";
  ogImage?: string;
  /** One or more schema.org objects rendered as JSON-LD <script> tags. */
  jsonLd?: object | object[];
}

function setMetaByName(name: string, content: string) {
  let el = document.querySelector<HTMLMetaElement>(`meta[name="${name}"]`);
  if (!el) {
    el = document.createElement("meta");
    el.setAttribute("name", name);
    document.head.appendChild(el);
  }
  el.setAttribute("content", content);
}

function setMetaByProperty(property: string, content: string) {
  let el = document.querySelector<HTMLMetaElement>(`meta[property="${property}"]`);
  if (!el) {
    el = document.createElement("meta");
    el.setAttribute("property", property);
    document.head.appendChild(el);
  }
  el.setAttribute("content", content);
}

function setCanonical(url: string) {
  let el = document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
  if (!el) {
    el = document.createElement("link");
    el.setAttribute("rel", "canonical");
    document.head.appendChild(el);
  }
  el.setAttribute("href", url);
}

function setJsonLd(jsonLd: object | object[] | undefined) {
  document.querySelectorAll('script[data-seo-jsonld="true"]').forEach((el) => el.remove());
  if (!jsonLd) return;
  const items = Array.isArray(jsonLd) ? jsonLd : [jsonLd];
  for (const item of items) {
    const script = document.createElement("script");
    script.type = "application/ld+json";
    script.setAttribute("data-seo-jsonld", "true");
    script.textContent = JSON.stringify(item);
    document.head.appendChild(script);
  }
}

/**
 * Imperatively manages document.title and <head> tags for the current route. A tiny hand-rolled
 * hook rather than react-helmet-async: this app renders a single active route at a time with no
 * concurrent/streaming head merging to manage, so a dependency buys little here.
 */
export function Seo({ title, description, canonicalPath, noindex, ogType = "website", ogImage, jsonLd }: SeoProps) {
  useEffect(() => {
    const fullTitle = title.includes(SITE_NAME) ? title : `${title} | ${SITE_NAME}`;
    document.title = fullTitle;

    const desc = description ?? DEFAULT_DESCRIPTION;
    setMetaByName("description", desc);
    setMetaByName("robots", noindex ? "noindex, nofollow" : "index, follow");

    const canonicalUrl = absoluteUrl(canonicalPath ?? window.location.pathname);
    setCanonical(canonicalUrl);

    const image = absoluteUrl(ogImage ?? DEFAULT_OG_IMAGE);
    setMetaByProperty("og:site_name", SITE_NAME);
    setMetaByProperty("og:title", fullTitle);
    setMetaByProperty("og:description", desc);
    setMetaByProperty("og:url", canonicalUrl);
    setMetaByProperty("og:type", ogType);
    setMetaByProperty("og:image", image);

    setMetaByName("twitter:card", "summary_large_image");
    setMetaByName("twitter:title", fullTitle);
    setMetaByName("twitter:description", desc);
    setMetaByName("twitter:image", image);

    setJsonLd(jsonLd);
  });

  return null;
}
