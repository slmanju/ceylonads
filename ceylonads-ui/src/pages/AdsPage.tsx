import { useSearchParams } from "react-router-dom";
import { AdSearchResults } from "../components/AdSearchResults/AdSearchResults";
import { Seo } from "../components/Seo/Seo";
import { useCategories } from "../hooks/useCategories";
import "./AdsPage.css";

export function AdsPage() {
  const [searchParams] = useSearchParams();
  const { categories } = useCategories();
  const page = Number(searchParams.get("page")) || 0;
  const categorySlug = searchParams.get("category") ?? "";
  const category = categorySlug ? categories.find((c) => c.slug === categorySlug) : undefined;

  const title = category ? `${category.name} for Sale in Sri Lanka` : "Browse Ads in Sri Lanka";
  const description = category
    ? `Browse ${category.name} ads for sale across Sri Lanka on CeylonAds. Find great deals from trusted sellers.`
    : "Browse thousands of ads for vehicles, property, mobiles, tuition and services across Sri Lanka on CeylonAds.";
  const heading = category ? category.name : "Browse Ads";

  // Category is kept in the canonical URL since it's a genuinely distinct, indexable search
  // (mirrors the old per-category page's SEO value); q/sort/location/price/attrs are deliberately
  // left out so crawlers consolidate those combinations onto one indexable page per category.
  const canonicalParams = new URLSearchParams();
  if (categorySlug) canonicalParams.set("category", categorySlug);
  if (page > 0) canonicalParams.set("page", String(page));
  const canonicalQuery = canonicalParams.toString();
  const canonicalPath = `/ads${canonicalQuery ? `?${canonicalQuery}` : ""}`;

  return (
    <div className="ads-page container">
      <Seo title={title} description={description} canonicalPath={canonicalPath} />
      <h1 className="ads-page__title">{heading}</h1>
      <AdSearchResults />
    </div>
  );
}
