import { useSearchParams } from "react-router-dom";
import { ClassSearchResults } from "../features/ClassSearch/ClassSearchResults";
import { Seo } from "../components/Seo/Seo";
import { useTuitionCategories } from "../hooks/useTuitionCategories";
import { useFeaturedTuition } from "../hooks/useFeaturedTuition";
import { FeaturedTuitionCarousel } from "../components/FeaturedTuitionCarousel/FeaturedTuitionCarousel";
import { SearchPromoCard } from "../components/SearchPromoCard/SearchPromoCard";
import { SearchPromoPlaceholderCard } from "../components/SearchPromoCard/SearchPromoPlaceholderCard";
import "./ClassesPage.css";

// Fixed page-level promotional inventory for the Tuition search page - its own slot
// (TUITION_SEARCH_TOP, "Search Page Featured" in the purchase catalog - see ceylonads-api's
// V18__tuition_promotion_catalog_v2.sql), independently configurable/purchasable from the
// homepage's TUITION_FEATURED carousel (see HomePage.tsx). Must be requested explicitly - without
// `slot`, useFeaturedTuition falls back to the backend's default category-featured resolution,
// which resolves to the *homepage's* TUITION_FEATURED slot, causing a Homepage Featured purchase
// to incorrectly render here too. Deliberately fetched with no search-filter args, so it stays
// identical regardless of subject/location/level - this is page advertising space, not a
// query-dependent result. Unsold slots backfill with SearchPromoPlaceholderCard so the section
// never shrinks or disappears. Distinct from the query-dependent "boosted" promotions rendered
// inside ClassSearchResults (search top banner + ad.promoted badges, which read TUITION_SEARCH_
// BOOST) - those relate to the current search, this section never does.
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
  const categorySlug = searchParams.get("category") ?? "";
  const category = categorySlug ? bySlug.get(categorySlug) : undefined;

  const heading = category ? category.name : "Browse Tuition Classes";
  const description = category
    ? `Find ${category.name.toLowerCase()} across Sri Lanka on ezClass.`
    : "Search tuition classes across every subject, grade and district in Sri Lanka.";

  const { featured: topPromotions, loading: topPromotionsLoading } = useFeaturedTuition(SEARCH_PAGE_FEATURED_SLOT_COUNT, {
    slot: SEARCH_PAGE_FEATURED_SLOT,
  });
  const topPromotionsPlaceholderCount = topPromotionsLoading
    ? 0
    : Math.max(0, SEARCH_PAGE_FEATURED_SLOT_COUNT - topPromotions.length);

  return (
    <div className="classes-page container">
      <Seo title={heading} description={description} />

      <section className="classes-page__top-promotions">
        <FeaturedTuitionCarousel
          items={topPromotions}
          loading={topPromotionsLoading}
          placeholderCount={topPromotionsPlaceholderCount}
          compact
          renderItem={(card) => <SearchPromoCard card={card} />}
          renderPlaceholder={() => <SearchPromoPlaceholderCard />}
        />
      </section>

      <ClassSearchResults heading={heading} />
    </div>
  );
}
