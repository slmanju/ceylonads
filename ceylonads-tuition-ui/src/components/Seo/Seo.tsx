import { useEffect } from "react";
import { DEFAULT_DESCRIPTION, DEFAULT_OG_IMAGE, SITE_NAME, absoluteUrl } from "../../utils/seo";

interface SeoProps {
  title: string;
  description?: string;
  noindex?: boolean;
  /** Path (with query string, e.g. "/classes?subject=ENGLISH") this page should canonicalize to.
   * Defaults to the current path+query when omitted. */
  canonicalPath?: string;
  /** Absolute image URL for Open Graph/Twitter cards. Falls back to no image tag when omitted. */
  ogImage?: string;
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

function setCanonicalLink(href: string) {
  let el = document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
  if (!el) {
    el = document.createElement("link");
    el.setAttribute("rel", "canonical");
    document.head.appendChild(el);
  }
  el.setAttribute("href", href);
}

/**
 * Imperatively manages document.title and a few <head> tags for the current route. Hand-rolled
 * rather than react-helmet-async: this app renders a single active route at a time, so a
 * dependency buys little here.
 */
export function Seo({ title, description, noindex, canonicalPath, ogImage }: SeoProps) {
  useEffect(() => {
    const fullTitle = title.includes(SITE_NAME) ? title : `${title} | ${SITE_NAME}`;
    const finalDescription = description ?? DEFAULT_DESCRIPTION;
    const canonicalUrl = absoluteUrl(canonicalPath ?? `${window.location.pathname}${window.location.search}`);

    document.title = fullTitle;
    setMetaByName("description", finalDescription);
    // "noindex, follow" (not nofollow) for faceted/search pages we don't want indexed - the links
    // on those pages (pagination, other filter combinations) should still be crawlable, only the
    // faceted page itself shouldn't rank. See CLAUDE.md's noindex policy.
    setMetaByName("robots", noindex ? "noindex, follow" : "index, follow");
    setCanonicalLink(canonicalUrl);

    setMetaByProperty("og:site_name", SITE_NAME);
    setMetaByProperty("og:type", "website");
    setMetaByProperty("og:title", title);
    setMetaByProperty("og:description", finalDescription);
    setMetaByProperty("og:url", canonicalUrl);
    setMetaByName("twitter:card", "summary_large_image");
    setMetaByName("twitter:title", title);
    setMetaByName("twitter:description", finalDescription);
    const finalImage = ogImage ?? DEFAULT_OG_IMAGE;
    setMetaByProperty("og:image", finalImage);
    setMetaByName("twitter:image", finalImage);
  });

  return null;
}
