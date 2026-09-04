import { useEffect, useRef, useState, type ReactNode } from "react";
import { useSearchParams } from "react-router-dom";
import { FaFilter } from "react-icons/fa";
import { useTuitionCategories } from "../../hooks/useTuitionCategories";
import { useLocations } from "../../hooks/useLocations";
import { useTuitionFilters } from "../../hooks/useTuitionFilters";
import { SubjectFilters } from "../../components/SubjectFilters/SubjectFilters";
import { FilterFooter } from "../../components/SubjectFilters/FilterFooter";
import { FiltersDrawer } from "../../components/FiltersDrawer/FiltersDrawer";
import { TuitionFilterBar } from "../../components/TuitionFilterBar/TuitionFilterBar";
import { ActiveFilterChips } from "../../components/ActiveFilterChips/ActiveFilterChips";
import { ClassGrid } from "../../components/ClassGrid/ClassGrid";
import { Pagination } from "../../components/Pagination/Pagination";
import type { AdResponse, SortOption } from "../../types/api";
import { tuitionRepository, searchTuitionClasses } from "../../tuition/api/tuitionApi";
import { useFeaturedTuition } from "../../hooks/useFeaturedTuition";
import { featuredCardToPromotion } from "../../tuition/promotion/api/tuitionPromotionApi";
import { SearchSpotlightRail } from "../../components/Promotion/SearchSpotlightRail";
import { SearchSpotlightInline } from "../../components/Promotion/SearchSpotlightInline";
import { emptyClassFilterValues, type ClassFilterValues } from "../../tuition/model/searchFilters";
import { matchesTuitionCriteria, type TuitionDetails, type TuitionSearchCriteria } from "../../tuition/model/tuition";
import { getApiErrorMessage } from "../../utils/apiError";
import "./ClassSearchResults.css";

// 3x3 grid: 3 columns x 3 rows = 9 organic results per page (see ClassGrid.css for the matching
// column layout). The backend default is also 9 (TuitionClassController#search), but this is
// passed explicitly so the page size never silently drifts if that default ever changes.
const PAGE_SIZE = 9;

// Tuition's "Search Boost" product (TUITION_SEARCH_BOOST) is a RANKING enhancement, not a separate
// placement: the backend (TuitionClassService.search / AdSearchService's slot-code overload) ranks
// a matching ad with a currently active Search Boost promotion first among these same organic
// results - never additive to PAGE_SIZE, never a separate fetch/section here. Each returned ad
// simply carries `promoted: true`, and ClassCard renders its normal PROMOTED badge (BoostedBadge)
// for it, exactly like any other card in this grid.

// Search Page Spotlight - the search page's fixed right-rail placement, a real,
// independently-purchasable slot reusing the stable TUITION_SEARCH_SIDEBAR_TOP code from before
// the six-product catalog cleanup, restored under its current name/price by ceylonads-api's V22
// migration. V25 raised its capacity from 1 to 12 concurrent advertisers, 4 of which are visible
// at once - see SearchSpotlightRail (desktop vertical carousel) and SearchSpotlightInline
// (mobile/tablet horizontal carousel), both fed by this same up-to-12-card fetch. Never mixed with
// Search Top (a separate advertising carousel, rendered above the filters in ClassesPage.tsx) or
// Search Boost (a ranking flag on the organic results above, not a placement of its own).
const SEARCH_SPOTLIGHT_SLOT = "TUITION_SEARCH_SIDEBAR_TOP";
const SEARCH_SPOTLIGHT_CAPACITY = 12;
// Where the mobile/tablet inline carousel lands among the organic cards - after the first row's
// worth (see ClassGrid's insertAfter). Desktop shows the same promotions in the right rail instead
// (see the >=1080px breakpoint in ClassSearchResults.css) - the two are mutually exclusive per
// breakpoint, never both visible at once.
const SEARCH_SPOTLIGHT_INLINE_INSERT_INDEX = 3;

// classFormat/classPurpose remain a decorative, mock-provider-only layer applied client-side to
// the already-fetched results page (see tuition/model/tuition.ts) - the backend has no such
// attributes yet. Every other filter (subject/level/curriculum/medium/deliveryMode) is now a real
// attr.<key> backend filter, wired in the search effect below.
function toTuitionCriteria(filters: ClassFilterValues): TuitionSearchCriteria {
  return {
    classFormats: filters.classFormats.length > 0 ? filters.classFormats : undefined,
    classPurposes: filters.classPurposes.length > 0 ? filters.classPurposes : undefined,
  };
}

const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: "newest", label: "Newest" },
  { value: "oldest", label: "Oldest" },
  { value: "price_asc", label: "Fee: Low to High" },
  { value: "price_desc", label: "Fee: High to Low" },
];

function readList(params: URLSearchParams, key: string): string[] {
  const raw = params.get(key);
  return raw ? raw.split(",").filter(Boolean) : [];
}

function readFilters(params: URLSearchParams): ClassFilterValues {
  return emptyClassFilterValues({
    q: params.get("q") ?? "",
    category: params.get("category") ?? "",
    location: params.get("location") ?? "",
    subject: params.get("subject") ?? "",
    level: params.get("level") ?? "",
    curriculum: params.get("curriculum") ?? "",
    medium: params.get("medium") ?? "",
    deliveryMode: params.get("deliveryMode") ?? "",
    minPrice: params.get("minPrice") ?? "",
    maxPrice: params.get("maxPrice") ?? "",
    classFormats: readList(params, "classFormat") as ClassFilterValues["classFormats"],
    classPurposes: readList(params, "classPurpose") as ClassFilterValues["classPurposes"],
  });
}

function readSort(params: URLSearchParams): SortOption {
  const value = params.get("sort");
  return SORT_OPTIONS.some((o) => o.value === value) ? (value as SortOption) : "newest";
}

function readPage(params: URLSearchParams): number {
  const value = Number(params.get("page"));
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 0;
}

export interface ClassSearchResultsProps {
  /** Page heading, rendered alongside the sort control - see the page-structure spec for the
   * tuition search layout (heading + sort share a row above the filter toolbar). */
  heading: string;
  /** Optional intro copy rendered under the heading row, above the filter toolbar. */
  intro?: ReactNode;
  /** Locks category to this slug and hides the class-type picker (e.g. Tutors, a subject page). */
  fixedCategorySlug?: string;
  /** Locks location to this slug and hides the district picker (e.g. a district landing page). */
  fixedLocationSlug?: string;
  /** Attribute filters always applied on top of user-chosen ones, e.g. classMode=ONLINE. Hidden from the UI. */
  fixedAttributeFilters?: Record<string, string>;
  emptyTitle?: string;
  emptyMessage?: string;
}

export function ClassSearchResults({
  heading,
  intro,
  fixedCategorySlug,
  fixedLocationSlug,
  fixedAttributeFilters,
  emptyTitle = "No classes match your search",
  emptyMessage = "Try a different subject, district, or clear some filters.",
}: ClassSearchResultsProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const { root: tuitionRoot } = useTuitionCategories();
  const { locations } = useLocations();
  const { data: tuitionFilters, loading: tuitionFiltersLoading } = useTuitionFilters();

  const [ads, setAds] = useState<AdResponse[]>([]);
  const [detailsById, setDetailsById] = useState<Map<number, TuitionDetails>>(new Map());
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [draftFilters, setDraftFilters] = useState<ClassFilterValues>(() => readFilters(searchParams));

  const activeFilters = readFilters(searchParams);
  const sort = readSort(searchParams);
  const page = readPage(searchParams);

  const effectiveCategory = fixedCategorySlug || activeFilters.category || tuitionRoot?.slug || "";
  const effectiveLocation = fixedLocationSlug || activeFilters.location;

  const { featured: spotlightFeatured, loading: spotlightLoading } = useFeaturedTuition(SEARCH_SPOTLIGHT_CAPACITY, {
    slot: SEARCH_SPOTLIGHT_SLOT,
  });
  const spotlightPromotions = spotlightFeatured.map((card) =>
    featuredCardToPromotion(card, "TUITION_SEARCH_SIDEBAR_TOP", "PROMOTED"),
  );

  const prevAppliedRef = useRef(activeFilters);
  useEffect(() => {
    const newApplied = readFilters(searchParams);
    setDraftFilters((prevDraft) => {
      const unedited = JSON.stringify(prevDraft) === JSON.stringify(prevAppliedRef.current);
      prevAppliedRef.current = newApplied;
      return unedited ? newApplied : prevDraft;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  useEffect(() => {
    if (!tuitionRoot) return;
    let cancelled = false;
    setLoading(true);
    setError(null);

    const attributeFilters: Record<string, string> = {};
    if (activeFilters.subject) attributeFilters["attr.subject"] = activeFilters.subject;
    if (activeFilters.level) attributeFilters["attr.grade"] = activeFilters.level;
    if (activeFilters.curriculum) attributeFilters["attr.curriculum"] = activeFilters.curriculum;
    if (activeFilters.medium) attributeFilters["attr.medium"] = activeFilters.medium;
    if (activeFilters.deliveryMode) attributeFilters["attr.classMode"] = activeFilters.deliveryMode;
    if (fixedAttributeFilters) {
      for (const [key, value] of Object.entries(fixedAttributeFilters)) {
        attributeFilters[`attr.${key}`] = value;
      }
    }

    const criteria = {
      q: activeFilters.q || undefined,
      category: effectiveCategory,
      location: effectiveLocation || undefined,
      minPrice: activeFilters.minPrice ? Number(activeFilters.minPrice) : undefined,
      maxPrice: activeFilters.maxPrice ? Number(activeFilters.maxPrice) : undefined,
      attributeFilters,
    };

    searchTuitionClasses({ ...criteria, page, size: PAGE_SIZE, sort })
      .then(async (data) => {
        if (cancelled) return;
        const map = await tuitionRepository.getDetailsMap(data.content, locations);
        if (cancelled) return;

        // Class Format/Class Purpose are mock-provider-only and applied client-side to this one
        // results page - see "Recommended flow" in the tuition enhancement spec. Search Boost
        // ranking already happened server-side (see the const comment above), so `data.content` is
        // rendered as-is, in the order the backend returned it - never reordered or filtered here.
        const tuitionCriteria = toTuitionCriteria(activeFilters);
        const filtered = data.content.filter((ad) => {
          const details = map.get(ad.id);
          return details ? matchesTuitionCriteria(details, tuitionCriteria) : true;
        });

        setAds(filtered);
        setDetailsById(map);
        setTotalElements(data.totalElements);
        setTotalPages(data.totalPages);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(getApiErrorMessage(err, "Could not load classes."));
        // Don't leave a previous successful search's count/pagination showing next to the error.
        setAds([]);
        setTotalElements(0);
        setTotalPages(0);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    tuitionRoot,
    effectiveCategory,
    effectiveLocation,
    activeFilters.q,
    activeFilters.subject,
    activeFilters.level,
    activeFilters.curriculum,
    activeFilters.medium,
    activeFilters.deliveryMode,
    activeFilters.minPrice,
    activeFilters.maxPrice,
    JSON.stringify(fixedAttributeFilters),
    JSON.stringify(toTuitionCriteria(activeFilters)),
    locations,
    sort,
    page,
  ]);

  const commitFilters = (filters: ClassFilterValues) => {
    const next = new URLSearchParams(searchParams);

    const trimmedQ = filters.q.trim();
    if (trimmedQ) next.set("q", trimmedQ);
    else next.delete("q");

    if (!fixedCategorySlug) {
      if (filters.category) next.set("category", filters.category);
      else next.delete("category");
    }

    if (!fixedLocationSlug) {
      if (filters.location) next.set("location", filters.location);
      else next.delete("location");
    }

    if (filters.subject) next.set("subject", filters.subject);
    else next.delete("subject");

    if (filters.level) next.set("level", filters.level);
    else next.delete("level");

    if (filters.curriculum) next.set("curriculum", filters.curriculum);
    else next.delete("curriculum");

    if (filters.medium) next.set("medium", filters.medium);
    else next.delete("medium");

    if (filters.deliveryMode) next.set("deliveryMode", filters.deliveryMode);
    else next.delete("deliveryMode");

    if (filters.minPrice) next.set("minPrice", filters.minPrice);
    else next.delete("minPrice");

    if (filters.maxPrice) next.set("maxPrice", filters.maxPrice);
    else next.delete("maxPrice");

    if (filters.classFormats.length > 0) next.set("classFormat", filters.classFormats.join(","));
    else next.delete("classFormat");

    if (filters.classPurposes.length > 0) next.set("classPurpose", filters.classPurposes.join(","));
    else next.delete("classPurpose");

    next.delete("page");
    setSearchParams(next);
  };

  // Primary filters (top bar, chips, mobile drawer selects) commit straight to the URL and
  // re-trigger the search immediately - no separate "Apply" step, per the tuition search spec.
  const applyPatch = (patch: Partial<ClassFilterValues>) => {
    commitFilters({ ...activeFilters, ...patch });
  };

  const applyDraft = () => {
    commitFilters(draftFilters);
    setDrawerOpen(false);
  };

  const resetDraft = () => {
    const cleared = emptyClassFilterValues({
      category: fixedCategorySlug ?? "",
      location: fixedLocationSlug ?? "",
    });
    setDraftFilters(cleared);
    commitFilters(cleared);
    setDrawerOpen(false);
  };

  const clearAll = () => {
    const cleared = emptyClassFilterValues({
      category: fixedCategorySlug ?? "",
      location: fixedLocationSlug ?? "",
    });
    commitFilters(cleared);
  };

  const changeSort = (value: SortOption) => {
    const next = new URLSearchParams(searchParams);
    next.set("sort", value);
    next.delete("page");
    setSearchParams(next);
  };

  const changePage = (nextPage: number) => {
    const next = new URLSearchParams(searchParams);
    if (nextPage > 0) next.set("page", String(nextPage));
    else next.delete("page");
    setSearchParams(next);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const activeFilterCount =
    [
      !fixedLocationSlug && activeFilters.location,
      activeFilters.subject,
      activeFilters.level,
      activeFilters.curriculum,
      activeFilters.medium,
      activeFilters.deliveryMode,
      activeFilters.minPrice,
      activeFilters.maxPrice,
    ].filter(Boolean).length +
    activeFilters.classFormats.length +
    activeFilters.classPurposes.length;

  const hasMockCriteria = activeFilters.classFormats.length > 0 || activeFilters.classPurposes.length > 0;

  const footer = <FilterFooter onReset={resetDraft} onApply={applyDraft} />;

  const sortControl = (
    <label className="class-search-results__sort">
      Sort:
      <select value={sort} onChange={(e) => changeSort(e.target.value as SortOption)}>
        {SORT_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );

  return (
    <div className="class-search-results">
      <div className="class-search-results__head">
        <h1 className="class-search-results__title">{heading}</h1>
        <div className="class-search-results__head-actions">
          <button type="button" className="btn btn-secondary class-search-results__filter-btn" onClick={() => setDrawerOpen(true)}>
            <FaFilter aria-hidden="true" />
            Filters
            {activeFilterCount > 0 && <span className="class-search-results__filter-badge">{activeFilterCount}</span>}
          </button>
          {sortControl}
        </div>
      </div>

      {intro && <p className="class-search-results__intro">{intro}</p>}

      <div className="class-search-results__toolbar">
        <TuitionFilterBar
          filters={tuitionFilters}
          filtersLoading={tuitionFiltersLoading}
          locations={locations}
          showLocationPicker={!fixedLocationSlug}
          values={activeFilters}
          onChange={applyPatch}
        />
      </div>

      <ActiveFilterChips
        values={activeFilters}
        filters={tuitionFilters}
        locations={locations}
        fixedLocationSlug={fixedLocationSlug}
        onChange={applyPatch}
        onClearAll={clearAll}
      />

      {!error && (
        <p className="class-search-results__count">
          {loading
            ? "Searching…"
            : hasMockCriteria
              ? `${ads.length} of ${totalElements} ${totalElements === 1 ? "class" : "classes"} on this page match`
              : `${totalElements} ${totalElements === 1 ? "class" : "classes"} found`}
          {activeFilters.q && <span className="class-search-results__query"> for "{activeFilters.q}"</span>}
        </p>
      )}

      <div className="class-search-results__body">
        <div className="class-search-results__main">
          <ClassGrid
            ads={ads}
            detailsById={detailsById}
            loading={loading}
            error={error}
            emptyTitle={emptyTitle}
            emptyMessage={emptyMessage}
            insertAfter={{
              index: SEARCH_SPOTLIGHT_INLINE_INSERT_INDEX,
              node: <SearchSpotlightInline promotions={spotlightFeatured} loading={spotlightLoading} />,
            }}
          />

          <Pagination page={page} totalPages={totalPages} onPageChange={changePage} />
        </div>

        <div className="class-search-results__spotlight-rail">
          <SearchSpotlightRail promotions={spotlightPromotions} loading={spotlightLoading} />
        </div>
      </div>

      <FiltersDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} footer={footer}>
        <SubjectFilters
          locations={locations}
          filters={tuitionFilters}
          filtersLoading={tuitionFiltersLoading}
          showLocationPicker={!fixedLocationSlug}
          values={draftFilters}
          onChange={setDraftFilters}
        />
      </FiltersDrawer>
    </div>
  );
}
