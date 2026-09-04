import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { SearchBar } from "../components/SearchBar/SearchBar";
import { SimilarClassCard } from "../components/SimilarClassCard/SimilarClassCard";
import { Pagination } from "../components/Pagination/Pagination";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { EmptyState } from "../components/EmptyState/EmptyState";
import { Seo } from "../components/Seo/Seo";
import { useLocations } from "../hooks/useLocations";
import { useHomepagePromotions } from "../hooks/useTuitionPromotions";
import { useFeaturedTuition } from "../hooks/useFeaturedTuition";
import { useLatestTuitionClasses } from "../hooks/useLatestTuitionClasses";
import { listTuitionPromotionPlans } from "../api/promotionApi";
import { featuredCardToPromotion } from "../tuition/promotion/api/tuitionPromotionApi";
import { PromotionBanner } from "../components/Promotion/PromotionBanner";
import { PromotionBannerSelfAd } from "../components/Promotion/PromotionBannerSelfAd";
import { HomeSpotlightRail } from "../components/Promotion/HomeSpotlightRail";
import { FeaturedTuitionCarousel } from "../components/FeaturedTuitionCarousel/FeaturedTuitionCarousel";
import "./HomePage.css";

// Homepage default feed: one paginated request to the isolated GET /api/tuition/classes endpoint
// (see ceylonads-api's TuitionClassService.getLatest) - see "Homepage" and "Performance" in the
// tuition CLAUDE.md. Pagination still moves through the rest via page/size on the same request.
const HOMEPAGE_PAGE_SIZE = 9;

// Temporarily hidden per request - flip back to true to restore the promo banner section.
const SHOW_PROMO_BANNER = false;

// The Featured Classes row reuses the shared CeylonAds CATEGORY_FEATURED promotion slot bound to
// the Education & Tuition category (TUITION_FEATURED in LocalDataSeeder) - the same slot
// /api/tuition/featured reads. `featuredSlotCount` only bounds how many *real* promotions to fetch
// (plan's slotCapacity, e.g. 12) - it is not a placeholder-backfill target. FeaturedTuitionCarousel
// fills its own first visible viewport from what it measures itself (see its ResizeObserver), and
// never pads beyond that just because the plan has more sellable capacity (see root CLAUDE.md's
// promotion placeholder spec). Falls back to this default when GET /api/tuition/promotions/plans
// is unreachable or that slot isn't seeded, so the fetch still requests a sensible number of cards.
const DEFAULT_FEATURED_SLOT_COUNT = 12;
const TUITION_FEATURED_CATEGORY_SLUG = "education-tuition";

// Homepage Spotlight, beside Latest Classes - its own real, independently-purchasable slot
// (TUITION_HOME_LATEST_RIGHT, capacity 8 / visible_count 4 in promotion master data), distinct
// from TUITION_FEATURED above - two separate paid products. A right-rail vertical carousel (see
// HomeSpotlightRail), not a single fixed card. `HOME_SPOTLIGHT_CAPACITY` only bounds how many real
// promotions to fetch, mirroring Search Page Spotlight's SEARCH_SPOTLIGHT_CAPACITY - the rail fills
// its own first visible viewport from what it measures, never padded beyond that just because the
// slot has more sellable capacity (see root CLAUDE.md's promotion placeholder spec).
const HOME_LATEST_RIGHT_SLOT = "TUITION_HOME_LATEST_RIGHT";
const HOME_SPOTLIGHT_CAPACITY = 12;

// Each quick link maps to a real ClassFilterValues param (see tuition/model/searchFilters.ts)
// so the destination /classes URL lands with the matching filter already selected in the
// filter bar and active-filter chips, not just a free-text q= search.
const HERO_QUICK_SEARCHES: {
  label: string;
  param: "level" | "curriculum" | "medium" | "deliveryMode" | "subject";
  value: string;
}[] = [
  { label: "A/L", param: "level", value: "AL" },
  { label: "O/L", param: "level", value: "OL" },
  { label: "Cambridge", param: "curriculum", value: "CAMBRIDGE" },
  { label: "English", param: "medium", value: "ENGLISH" },
  { label: "Online", param: "deliveryMode", value: "ONLINE" },
];

// Real crawlable links into the SEO-worthy subject/delivery landing pages (see
// src/utils/searchSeo.ts) - distinct from HERO_QUICK_SEARCHES above (existing generic
// level/curriculum/medium shortcuts), so Google can discover these /classes?... URLs from the
// homepage without depending solely on the sitemap.
const SEO_QUICK_LINKS: { label: string; param: "subject" | "deliveryMode"; value: string }[] = [
  { label: "English Classes", param: "subject", value: "ENGLISH" },
  { label: "Maths Classes", param: "subject", value: "MATHEMATICS" },
  { label: "Chess Classes", param: "subject", value: "CHESS" },
  { label: "Home Visit Classes", param: "deliveryMode", value: "HOME_VISIT" },
];

export function HomePage() {
  const { locations } = useLocations();

  const [page, setPage] = useState(0);
  const {
    classes: latestClasses,
    totalPages: latestTotalPages,
    loading: latestLoading,
    error: latestError,
  } = useLatestTuitionClasses(page, HOMEPAGE_PAGE_SIZE);

  const { topBanner: promoBanner } = useHomepagePromotions();

  const { featured: homeSpotlightFeatured, loading: homeSpotlightLoading } = useFeaturedTuition(HOME_SPOTLIGHT_CAPACITY, {
    slot: HOME_LATEST_RIGHT_SLOT,
  });
  const homeSpotlightPromotions = homeSpotlightFeatured.map((card) =>
    featuredCardToPromotion(card, "TUITION_HOME_LATEST_RIGHT"),
  );

  const [featuredSlotCount, setFeaturedSlotCount] = useState(DEFAULT_FEATURED_SLOT_COUNT);
  useEffect(() => {
    let cancelled = false;
    listTuitionPromotionPlans()
      .then((plans) => {
        const tuitionFeaturedPlan = plans.find(
          ({ plan }) => plan.placementType === "CATEGORY_FEATURED" && plan.categorySlug === TUITION_FEATURED_CATEGORY_SLUG,
        );
        if (!cancelled && tuitionFeaturedPlan) setFeaturedSlotCount(tuitionFeaturedPlan.plan.slotCapacity);
      })
      .catch(() => {
        // Powers only how many Featured Classes placeholder cards to backfill - the default above
        // is a fine fallback, never worth an error state.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const { featured: featuredTuition, loading: featuredTuitionLoading } = useFeaturedTuition(featuredSlotCount);

  return (
    <div className="tuition-home">
      <Seo
        title="Tuition Classes, Tutors & Panthi in Sri Lanka"
        description="Find tuition classes, tutors and panthi across Sri Lanka. Search online, physical and home-visit classes for English, maths, science, chess and more."
      />

      <section className="tuition-hero">
        <div className="container tuition-hero__inner">
          <div className="tuition-hero__content">
            <span className="tuition-hero__eyebrow">Sri Lanka's Tuition Marketplace</span>
            <h1 className="tuition-hero__title">Find the right class, tutor or course near you or online</h1>
            <p className="tuition-hero__subtitle">
              Search thousands of tuition classes across every subject, grade and district in Sri Lanka.
            </p>

            <SearchBar locations={locations} />

            <ul className="tuition-hero__quick-links">
              {HERO_QUICK_SEARCHES.map(({ label, param, value }) => (
                <li key={label}>
                  <Link to={`/classes?${param}=${encodeURIComponent(value)}`} className="tuition-hero__quick-link">
                    {label}
                  </Link>
                </li>
              ))}
            </ul>

            <Link to="/post-ad" className="tuition-hero__cta">
              Are you a tutor? Post your class for free&nbsp;→
            </Link>
          </div>

          <div className="tuition-hero__media" aria-hidden="true">
            <img
              src="/images/tuition-hero.png"
              alt=""
              className="tuition-hero__media-image"
              width={900}
              height={675}
            />
          </div>
        </div>
      </section>

      {SHOW_PROMO_BANNER && (
        <section className="container tuition-home__section tuition-home__section--banner">
          {promoBanner ? <PromotionBanner promotion={promoBanner} size="large" /> : <PromotionBannerSelfAd />}
        </section>
      )}

      <section className="container tuition-home__section">
        <div className="tuition-home__section-header">
          <h2>Browse by Subject</h2>
        </div>
        <ul className="tuition-hero__quick-links">
          {SEO_QUICK_LINKS.map(({ label, param, value }) => (
            <li key={label}>
              <Link to={`/classes?${param}=${encodeURIComponent(value)}`} className="tuition-hero__quick-link">
                {label}
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section className="container tuition-home__section">
        <FeaturedTuitionCarousel
          title="Featured Classes"
          items={featuredTuition}
          loading={featuredTuitionLoading}
          placeholderKeyPrefix="home-featured-placeholder"
        />
      </section>

      <section className="container tuition-home__section">
        <div className="tuition-home__section-header">
          <h2>Latest Classes</h2>
          <Link to="/classes" className="tuition-home__section-link">
            View all classes →
          </Link>
        </div>
        <div className="tuition-home__latest-layout">
          <div className="tuition-home__latest-main">
            {latestLoading && <LoadingState label="Loading classes…" />}
            {!latestLoading && latestError && <ErrorState message={latestError} />}
            {!latestLoading && !latestError && latestClasses.length === 0 && (
              <EmptyState title="No classes yet" message="Check back soon for new tuition classes." />
            )}
            {!latestLoading && !latestError && latestClasses.length > 0 && (
              <div className="tuition-home__latest-grid">
                {latestClasses.map((card) => (
                  <SimilarClassCard key={card.id} card={card} />
                ))}
              </div>
            )}
            <Pagination page={page} totalPages={latestTotalPages} onPageChange={setPage} />
          </div>
          <div className="tuition-home__latest-side">
            <HomeSpotlightRail promotions={homeSpotlightPromotions} loading={homeSpotlightLoading} />
          </div>
        </div>
      </section>
    </div>
  );
}
