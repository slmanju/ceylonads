import { useSearchParams } from "react-router-dom";
import { ClassSearchResults } from "../features/ClassSearch/ClassSearchResults";
import { Seo } from "../components/Seo/Seo";
import { JsonLd } from "../components/Seo/JsonLd";
import { useTuitionCategories } from "../hooks/useTuitionCategories";
import { useLocations } from "../hooks/useLocations";
import { useFeaturedTuition } from "../hooks/useFeaturedTuition";
import { FeaturedTuitionCarousel } from "../components/FeaturedTuitionCarousel/FeaturedTuitionCarousel";
import { SearchPromoCard } from "../components/SearchPromoCard/SearchPromoCard";
import { SearchPromoPlaceholderCard } from "../components/SearchPromoCard/SearchPromoPlaceholderCard";
import { decideSearchSeo, buildCanonicalSearchPath, buildSearchSeoContent } from "../utils/searchSeo";
import { breadcrumbListJsonLd } from "../utils/structuredData";
import "./ClassesPage.css";

// Fixed page-level promotional inventory for the Tuition search page - its own slot
// (TUITION_SEARCH_TOP, "Search Page Featured" in the purchase catalog - see ceylonads-api's
// V18__tuition_promotion_catalog_v2.sql), independently configurable/purchasable from the
// homepage's TUITION_FEATURED carousel (see HomePage.tsx). Must be requested explicitly - without
// `slot`, useFeaturedTuition falls back to the backend's default category-featured resolution,
// which resolves to the *homepage's* TUITION_FEATURED slot, causing a Homepage Featured purchase
// to incorrectly render here too. Deliberately fetched with no search-filter args, so it stays
// identical regardless of subject/location/level - this is page advertising space, not a
// query-dependent result. `SEARCH_PAGE_FEATURED_SLOT_COUNT` only bounds how many *real* promotions
// to fetch - FeaturedTuitionCarousel fills its own first visible viewport with
// SearchPromoPlaceholderCard from what it measures itself, and never pads beyond that (see root
// CLAUDE.md's promotion placeholder spec). Distinct from the query-dependent "boosted" promotions
// rendered inside ClassSearchResults (leading the results list + Spotlight rail/inline card, which
// read TUITION_SEARCH_BOOST/TUITION_SEARCH_SIDEBAR_TOP) - those relate to the current search, this
// section never does.
//
// Renders via the same FeaturedTuitionCarousel as the homepage, but in `compact` mode with the
// SearchPromoCard/SearchPromoPlaceholderCard renderers - a short landscape card instead of the
// homepage's tall portrait FeaturedTuitionCard - and with no `title`, so the carousel sits
// directly on the page's white background with no header row: just cards, edge arrows and dots.
// Each card's own PROMOTED badge already communicates that this is promotional inventory, so no
// section-level heading or outer panel is needed on top of it.
const SEARCH_PAGE_FEATURED_SLOT = "TUITION_SEARCH_TOP";
const SEARCH_PAGE_FEATURED_SLOT_COUNT = 12;

export function ClassesPage() {
  const [searchParams] = useSearchParams();
  const { bySlug } = useTuitionCategories();
  const { locations } = useLocations();
  const categorySlug = searchParams.get("category") ?? "";
  const category = categorySlug ? bySlug.get(categorySlug) : undefined;

  // Category-based browsing predates this SEO work and isn't one of the three approved SEO
  // dimensions (subject/deliveryMode/location) - left entirely as-is (own title/description, no
  // canonical/noindex override) rather than folded into the new rule.
  const seoDecision = decideSearchSeo(searchParams);
  const locationSlug = seoDecision.canonicalLocation;
  const locationName = locationSlug ? locations.find((l) => l.slug === locationSlug)?.name : undefined;
  const seoContent = buildSearchSeoContent({
    subjectCode: seoDecision.canonicalSubject,
    deliveryMode: seoDecision.canonicalDeliveryMode,
    locationName,
  });
  const canonicalPath = buildCanonicalSearchPath(seoDecision);
  const hasSeoDimension = !!(
    seoDecision.canonicalSubject ||
    seoDecision.canonicalDeliveryMode ||
    seoDecision.canonicalLocation
  );

  const heading = category ? category.name : hasSeoDimension ? seoContent.h1 : "Browse Tuition Classes";
  const description = category ? `Find ${category.name.toLowerCase()} across Sri Lanka on ezClass.` : seoContent.description;

  const { featured: topPromotions, loading: topPromotionsLoading } = useFeaturedTuition(SEARCH_PAGE_FEATURED_SLOT_COUNT, {
    slot: SEARCH_PAGE_FEATURED_SLOT,
  });

  const breadcrumbItems = [
    { name: "Home", path: "/" },
    { name: "Classes", path: "/classes" },
  ];
  if (!category && hasSeoDimension && seoDecision.indexable) {
    breadcrumbItems.push({ name: seoContent.h1, path: canonicalPath });
  }

  return (
    <div className="classes-page container">
      <Seo
        title={category ? category.name : seoContent.title}
        description={description}
        noindex={category ? undefined : !seoDecision.indexable}
        canonicalPath={category ? undefined : canonicalPath}
      />
      <JsonLd id="classes-breadcrumb" data={breadcrumbListJsonLd(breadcrumbItems)} />

      <section className="classes-page__top-promotions">
        <FeaturedTuitionCarousel
          items={topPromotions}
          loading={topPromotionsLoading}
          placeholderKeyPrefix="search-featured-placeholder"
          compact
          renderItem={(card) => <SearchPromoCard card={card} />}
          renderPlaceholder={() => <SearchPromoPlaceholderCard />}
        />
      </section>

      <ClassSearchResults
        heading={heading}
        intro={!category && seoDecision.indexable && hasSeoDimension ? seoContent.intro : undefined}
      />
    </div>
  );
}
