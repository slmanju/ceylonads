import type { TuitionFeaturedCardResponse } from "../../types/api";
import { SearchPromoCard } from "../SearchPromoCard/SearchPromoCard";
import "./SearchBoostSection.css";

interface SearchBoostSectionProps {
  items: TuitionFeaturedCardResponse[];
  loading: boolean;
}

// TUITION_SEARCH_BOOST's presentation: real boosted listings only, laid out like a short row of
// search results (see SearchPromoCard) rather than the page-level FeaturedTuitionCarousel used by
// TUITION_SEARCH_TOP - no arrows/dots, and never backfilled with placeholder cards. Renders
// nothing while loading or when there are zero active Search Boost promotions, so organic results
// move straight up under the result count instead of leaving a gap.
export function SearchBoostSection({ items, loading }: SearchBoostSectionProps) {
  if (loading || items.length === 0) {
    return null;
  }

  return (
    <section className="search-boost-section" aria-label="Promoted classes">
      <span className="search-boost-section__label">Promoted</span>
      <div className="search-boost-section__grid">
        {items.map((item) => (
          <SearchPromoCard key={item.id} card={item} />
        ))}
      </div>
    </section>
  );
}
