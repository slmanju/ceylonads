import { useEffect } from "react";
import { DEFAULT_DESCRIPTION, SITE_NAME } from "../../utils/seo";

interface SeoProps {
  title: string;
  description?: string;
  noindex?: boolean;
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

/**
 * Imperatively manages document.title and a few <head> tags for the current route. Hand-rolled
 * rather than react-helmet-async: this app renders a single active route at a time, so a
 * dependency buys little here.
 */
export function Seo({ title, description, noindex }: SeoProps) {
  useEffect(() => {
    const fullTitle = title.includes(SITE_NAME) ? title : `${title} | ${SITE_NAME}`;
    document.title = fullTitle;
    setMetaByName("description", description ?? DEFAULT_DESCRIPTION);
    setMetaByName("robots", noindex ? "noindex, nofollow" : "index, follow");
  });

  return null;
}
