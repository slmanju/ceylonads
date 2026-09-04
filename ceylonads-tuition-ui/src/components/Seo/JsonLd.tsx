import { useEffect } from "react";

interface JsonLdProps {
  /** Unique per call site (e.g. "site", "search", "class-detail") - lets independent JSON-LD
   * blocks (site-wide Organization/WebSite in AppLayout vs. per-page Course/BreadcrumbList) coexist
   * without one overwriting another. */
  id: string;
  data: object | object[];
}

/**
 * Injects a <script type="application/ld+json"> tag, imperatively like Seo.tsx. Removed on
 * unmount so a page-level block (e.g. a class's Course schema) never lingers into the next route
 * a site-wide block (Organization/WebSite, mounted once in AppLayout) is never unmounted.
 */
export function JsonLd({ id, data }: JsonLdProps) {
  useEffect(() => {
    const elementId = `jsonld-${id}`;
    let el = document.getElementById(elementId) as HTMLScriptElement | null;
    if (!el) {
      el = document.createElement("script");
      el.id = elementId;
      el.type = "application/ld+json";
      document.head.appendChild(el);
    }
    el.textContent = JSON.stringify(data);

    return () => {
      el?.remove();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, JSON.stringify(data)]);

  return null;
}
