import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FaPlus, FaStar } from "react-icons/fa";
import { SearchBar } from "../components/SearchBar/SearchBar";
import { CategoryCard } from "../components/CategoryCard/CategoryCard";
import { AdGrid } from "../components/AdGrid/AdGrid";
import { AdCarousel } from "../components/Carousel/AdCarousel";
import { BannerCarousel } from "../components/Carousel/BannerCarousel";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import { Seo } from "../components/Seo/Seo";
import { useCategories } from "../hooks/useCategories";
import { useLocations } from "../hooks/useLocations";
import { searchAds, getFeaturedAds } from "../api/adsApi";
import { getActiveBanners, listActivePromotionPlans } from "../api/promotionApi";
import { formatPrice } from "../utils/formatPrice";
import type { AdResponse, PromotionBannerResponse, PromotionPlanResponse } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";
import { absoluteUrl } from "../utils/seo";
import "./HomePage.css";

const HOME_JSON_LD = [
  {
    "@context": "https://schema.org",
    "@type": "Organization",
    name: "CeylonAds",
    url: absoluteUrl("/"),
    description: "Sri Lanka's marketplace to buy and sell vehicles, property, mobiles, tuition and services.",
  },
  {
    "@context": "https://schema.org",
    "@type": "WebSite",
    name: "CeylonAds",
    url: absoluteUrl("/"),
    potentialAction: {
      "@type": "SearchAction",
      target: `${absoluteUrl("/ads")}?q={search_term_string}`,
      "query-input": "required name=search_term_string",
    },
  },
];

// Generous upper bound for how many HOME_FEATURED ads to fetch for the carousel; the backend
// clamps the actual result to the slot's own capacity regardless of what's requested here.
const FEATURED_FETCH_LIMIT = 50;
const DEFAULT_FEATURED_VISIBLE_COUNT = 4;

// Temporarily hidden on the homepage; keep the sections/data-fetching intact for future use.
const SHOW_BROWSE_CATEGORIES = false;
const SHOW_FEATURED_ADS = false;

export function HomePage() {
  const { categories, loading: categoriesLoading, error: categoriesError } = useCategories();
  const { locations } = useLocations();

  const [ads, setAds] = useState<AdResponse[]>([]);
  const [adsLoading, setAdsLoading] = useState(true);
  const [adsError, setAdsError] = useState<string | null>(null);

  const [featuredAds, setFeaturedAds] = useState<AdResponse[]>([]);
  const [featuredLoading, setFeaturedLoading] = useState(true);
  const [featuredError, setFeaturedError] = useState<string | null>(null);

  const [banners, setBanners] = useState<PromotionBannerResponse[]>([]);
  const [bannerChecked, setBannerChecked] = useState(false);
  const [plans, setPlans] = useState<PromotionPlanResponse[]>([]);

  const bannerPlan = plans.find((p) => p.placementType === "HOME_BANNER") ?? null;
  const featuredPlan = plans.find((p) => p.placementType === "HOME_FEATURED") ?? null;
  const featuredVisibleCount = featuredPlan?.slotVisibleCount ?? DEFAULT_FEATURED_VISIBLE_COUNT;

  const topLevelCategories = categories.filter((c) => c.parentId === null);

  useEffect(() => {
    let cancelled = false;
    setAdsLoading(true);
    setAdsError(null);

    searchAds({ size: 10, sort: "newest" })
      .then((data) => {
        if (!cancelled) setAds(data.content);
      })
      .catch((err) => {
        if (!cancelled) setAdsError(getApiErrorMessage(err, "Could not load ads."));
      })
      .finally(() => {
        if (!cancelled) setAdsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setFeaturedLoading(true);
    setFeaturedError(null);

    getFeaturedAds(FEATURED_FETCH_LIMIT)
      .then((data) => {
        if (!cancelled) setFeaturedAds(data);
      })
      .catch((err) => {
        if (!cancelled) setFeaturedError(getApiErrorMessage(err, "Could not load featured ads."));
      })
      .finally(() => {
        if (!cancelled) setFeaturedLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    getActiveBanners("HOME_BANNER")
      .then((activeBanners) => {
        if (!cancelled) setBanners(activeBanners);
      })
      .catch(() => {
        // A banner is a nice-to-have on the homepage; silently skip it on failure rather than
        // showing an error state for a non-essential section.
      })
      .finally(() => {
        if (!cancelled) setBannerChecked(true);
      });

    // Powers the promotion-placeholder pricing (homepage banner + featured slots), so a
    // failure here just means placeholders fall back to example copy - never worth an error state.
    listActivePromotionPlans()
      .then((data) => {
        if (!cancelled) setPlans(data);
      })
      .catch(() => {});

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="home-page">
      <Seo
        title="CeylonAds — Buy & Sell in Sri Lanka"
        description="Sri Lanka's trusted marketplace to buy and sell vehicles, property, mobiles, tuition and services. Post a free ad today."
        canonicalPath="/"
        jsonLd={HOME_JSON_LD}
      />
      <section className="home-hero">
        <div className="container home-hero__inner">
          <h1 className="home-hero__title">Find what you're looking for in Sri Lanka</h1>
          <p className="home-hero__subtitle">Buy, sell and discover great deals across the island.</p>

          <SearchBar categories={categories} locations={locations} />

          <Link to="/post-ad" className="btn btn-primary home-hero__cta">
            <FaPlus aria-hidden="true" />
            Post Free Ad
          </Link>
        </div>
      </section>

      {banners.length > 0 && (
        <section className="container home-banner">
          <BannerCarousel banners={banners} />
        </section>
      )}

      {/* Local-development-only placeholder: never shown in a production build, since
          import.meta.env.DEV is compiled to false by `npm run build`. */}
      {banners.length === 0 && bannerChecked && import.meta.env.DEV && (
        <section className="container home-banner">
          <Link to="/my-ads" className="home-banner__placeholder">
            <span className="home-banner__placeholder-tag">Advertise Here (dev preview)</span>
            <span className="home-banner__placeholder-title">Promote Your Business Here</span>
            <span className="home-banner__placeholder-copy">
              {bannerPlan ? `${bannerPlan.durationDays} Days — ${formatPrice(bannerPlan.price)}` : "7 Days — Rs. 5,000"}
            </span>
            <span className="home-banner__placeholder-cta">Promote Your Business →</span>
          </Link>
        </section>
      )}

      {SHOW_BROWSE_CATEGORIES && (
        <section className="container home-section">
          <div className="home-section__header">
            <h2>Browse Categories</h2>
            <Link to="/ads" className="home-section__link">
              View all categories →
            </Link>
          </div>

          {categoriesLoading && <LoadingState label="Loading categories…" />}
          {categoriesError && <ErrorState message={categoriesError} />}
          {!categoriesLoading && !categoriesError && (
            <div className="category-grid">
              {topLevelCategories.map((category) => (
                <CategoryCard key={category.id} category={category} />
              ))}
            </div>
          )}
        </section>
      )}

      {SHOW_FEATURED_ADS && (
        <section className="container home-section home-section--featured">
          <div className="home-section__header">
            <h2>
              <FaStar className="home-section__star" aria-hidden="true" /> Featured Ads
            </h2>
          </div>
          <p className="home-section__subtext">Highlighted listings from across CeylonAds</p>

          {featuredLoading && <LoadingState label="Loading featured ads…" />}
          {featuredError && <ErrorState message={featuredError} />}
          {!featuredLoading && !featuredError && (
            <AdCarousel
              ads={featuredAds}
              visibleCount={featuredVisibleCount}
              labelPrefix="Featured ads"
              placeholder={{
                title: "Promote Your Ad Here",
                subtitle: "Homepage Featured",
                priceLabel: featuredPlan
                  ? `${featuredPlan.durationDays} Days — ${formatPrice(featuredPlan.price)}`
                  : "7 Days — Rs. 750",
              }}
            />
          )}
        </section>
      )}

      <section className="container home-section">
        <div className="home-section__header">
          <h2>Latest Ads</h2>
          <Link to="/ads" className="home-section__link">
            View all ads →
          </Link>
        </div>
        <p className="home-section__subtext">New ads posted near you</p>
        <AdGrid ads={ads} loading={adsLoading} error={adsError} emptyTitle="No ads posted yet" />
      </section>
    </div>
  );
}
