import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { FaFilter } from "react-icons/fa";
import { searchAds, searchTuitionClasses } from "../../api/adsApi";
import { useCategories } from "../../hooks/useCategories";
import { useLocations } from "../../hooks/useLocations";
import { useCategoryFilters } from "../../hooks/useCategoryFilters";
import { FiltersPanel, type FilterValues } from "../FiltersPanel/FiltersPanel";
import { FiltersDrawer } from "../FiltersDrawer/FiltersDrawer";
import { FilterFooter } from "../FilterFooter/FilterFooter";
import { AdGrid } from "../AdGrid/AdGrid";
import { Pagination } from "../Pagination/Pagination";
import type { AdResponse, SortOption } from "../../types/api";
import { getApiErrorMessage } from "../../utils/apiError";
import { categoryAncestors } from "../../utils/categoryHierarchy";
import "./AdSearchResults.css";

const PAGE_SIZE = 20;
// Tuition search results render a fixed 3x3 grid (3 columns x 3 rows) instead of the generic
// page size - see AdGrid's "tuition" variant for the matching column layout.
const TUITION_PAGE_SIZE = 9;
const ATTR_PREFIX = "attr.";

const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: "newest", label: "Newest" },
  { value: "oldest", label: "Oldest" },
  { value: "price_asc", label: "Price: Low to High" },
  { value: "price_desc", label: "Price: High to Low" },
];

function readFilters(params: URLSearchParams): FilterValues {
  const attributes: Record<string, string> = {};
  for (const [key, value] of params.entries()) {
    if (key.startsWith(ATTR_PREFIX)) {
      attributes[key.slice(ATTR_PREFIX.length)] = value;
    }
  }

  return {
    q: params.get("q") ?? "",
    category: params.get("category") ?? "",
    location: params.get("location") ?? "",
    minPrice: params.get("minPrice") ?? "",
    maxPrice: params.get("maxPrice") ?? "",
    attributes,
  };
}

function readSort(params: URLSearchParams): SortOption {
  const value = params.get("sort");
  return SORT_OPTIONS.some((o) => o.value === value) ? (value as SortOption) : "newest";
}

function readPage(params: URLSearchParams): number {
  const value = Number(params.get("page"));
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 0;
}

function toAttributeFilters(attributes: Record<string, string>): Record<string, string> | undefined {
  const entries = Object.entries(attributes)
    .filter(([, value]) => value !== "")
    .map(([key, value]) => [`${ATTR_PREFIX}${key}`, value] as const);
  return entries.length > 0 ? Object.fromEntries(entries) : undefined;
}

export function AdSearchResults() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { categories, loading: categoriesLoading } = useCategories();
  const { locations } = useLocations();

  const [ads, setAds] = useState<AdResponse[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [draftFilters, setDraftFilters] = useState<FilterValues>(() => readFilters(searchParams));

  const activeFilters = readFilters(searchParams);
  const sort = readSort(searchParams);
  const page = readPage(searchParams);

  // Same rule AdCard uses to switch to the portrait poster treatment: any category whose root
  // ancestor is Education & Tuition routes to the isolated Tuition search endpoint/page size/grid.
  const selectedCategory = activeFilters.category
    ? categories.find((c) => c.slug === activeFilters.category)
    : undefined;
  const isTuition = selectedCategory
    ? categoryAncestors(categories, selectedCategory)[0]?.slug === "education-tuition"
    : false;
  const pageSize = isTuition ? TUITION_PAGE_SIZE : PAGE_SIZE;

  const {
    definitions: attributeDefinitions,
    loading: attributeDefinitionsLoading,
    error: attributeDefinitionsError,
  } = useCategoryFilters(draftFilters.category);

  // Only auto-follow external URL changes (back/forward, applying) when the draft has no unsaved
  // edits relative to the previously-applied filters - otherwise a sort/page change made while
  // mid-edit (e.g. on the desktop sidebar) would silently discard the user's in-progress draft.
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
    // Categories must be loaded first so isTuition reflects the real category tree - otherwise a
    // direct link into a Tuition category would briefly fetch from the generic /api/ads endpoint
    // (wrong page size/grid) before flipping over once categories arrive.
    if (categoriesLoading) return;

    let cancelled = false;
    setLoading(true);
    setError(null);

    const fetchAds = isTuition ? searchTuitionClasses : searchAds;
    fetchAds({
      q: activeFilters.q || undefined,
      category: activeFilters.category || undefined,
      location: activeFilters.location || undefined,
      minPrice: activeFilters.minPrice ? Number(activeFilters.minPrice) : undefined,
      maxPrice: activeFilters.maxPrice ? Number(activeFilters.maxPrice) : undefined,
      page,
      size: pageSize,
      sort,
      attributeFilters: toAttributeFilters(activeFilters.attributes),
    })
      .then((data) => {
        if (cancelled) return;
        setAds(data.content);
        setTotalElements(data.totalElements);
        setTotalPages(data.totalPages);
      })
      .catch((err) => {
        if (!cancelled) setError(getApiErrorMessage(err, "Could not load ads."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    activeFilters.q,
    activeFilters.category,
    activeFilters.location,
    activeFilters.minPrice,
    activeFilters.maxPrice,
    JSON.stringify(activeFilters.attributes),
    sort,
    page,
    isTuition,
    pageSize,
    categoriesLoading,
  ]);

  const commitFilters = (filters: FilterValues) => {
    const next = new URLSearchParams(searchParams);

    const trimmedQ = filters.q.trim();
    if (trimmedQ) next.set("q", trimmedQ);
    else next.delete("q");

    if (filters.category) next.set("category", filters.category);
    else next.delete("category");

    if (filters.location) next.set("location", filters.location);
    else next.delete("location");

    if (filters.minPrice) next.set("minPrice", filters.minPrice);
    else next.delete("minPrice");

    if (filters.maxPrice) next.set("maxPrice", filters.maxPrice);
    else next.delete("maxPrice");

    for (const key of [...next.keys()]) {
      if (key.startsWith(ATTR_PREFIX)) next.delete(key);
    }
    for (const [key, value] of Object.entries(filters.attributes)) {
      if (value) next.set(`${ATTR_PREFIX}${key}`, value);
    }

    next.delete("page");
    setSearchParams(next);
    setDrawerOpen(false);
  };

  const applyDraft = () => commitFilters(draftFilters);

  const resetDraft = () => {
    setDraftFilters({
      q: "",
      category: "",
      location: "",
      minPrice: "",
      maxPrice: "",
      attributes: {},
    });
  };

  const closeDrawer = () => {
    setDraftFilters(readFilters(searchParams));
    setDrawerOpen(false);
  };

  // Category is the primary discovery dimension (mirrors the old dedicated category route), so
  // selecting one applies immediately rather than waiting for the Apply button that Location/Price
  // use - switching from Vehicles to Property should feel as instant as the old direct navigation.
  const handleCategoryChange = (slug: string) => {
    if (draftFilters.category === slug) return;
    const next = { ...draftFilters, category: slug, attributes: {} };
    setDraftFilters(next);
    commitFilters(next);
  };

  const handleLocationChange = (slug: string) => {
    setDraftFilters((prev) => ({ ...prev, location: slug }));
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
    [activeFilters.category, activeFilters.location, activeFilters.minPrice, activeFilters.maxPrice].filter(Boolean)
      .length + Object.values(activeFilters.attributes).filter(Boolean).length;

  const footer = <FilterFooter onReset={resetDraft} onApply={applyDraft} />;

  return (
    <div className="ad-search-results">
      <aside className="ad-search-results__sidebar">
        <h2 className="ad-search-results__sidebar-title">Filters</h2>
        <FiltersPanel
          categories={categories}
          locations={locations}
          attributeDefinitions={attributeDefinitions}
          attributeDefinitionsLoading={attributeDefinitionsLoading}
          attributeDefinitionsError={attributeDefinitionsError}
          values={draftFilters}
          onChange={setDraftFilters}
          onCategoryChange={handleCategoryChange}
          onLocationChange={handleLocationChange}
          onSubmit={applyDraft}
        />
        <div className="ad-search-results__sidebar-footer">{footer}</div>
      </aside>

      <div className="ad-search-results__main">
        <div className="ad-search-results__toolbar">
          <label className="ad-search-results__sort">
            Sort:
            <select value={sort} onChange={(e) => changeSort(e.target.value as SortOption)}>
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <button
            type="button"
            className="btn btn-secondary ad-search-results__filter-btn"
            onClick={() => setDrawerOpen(true)}
          >
            <FaFilter aria-hidden="true" />
            Filters
            {activeFilterCount > 0 && <span className="ad-search-results__filter-badge">{activeFilterCount}</span>}
          </button>
        </div>

        <p className="ad-search-results__count">
          {loading ? "Searching…" : `${totalElements} result${totalElements === 1 ? "" : "s"}`}
          {activeFilters.q && <span className="ad-search-results__query"> for "{activeFilters.q}"</span>}
        </p>

        <AdGrid
          ads={ads}
          loading={loading}
          error={error}
          emptyTitle="No ads match your search"
          emptyMessage="Try a different keyword, category, or location."
          variant={isTuition ? "tuition" : "default"}
        />

        <Pagination page={page} totalPages={totalPages} onPageChange={changePage} />
      </div>

      <FiltersDrawer open={drawerOpen} onClose={closeDrawer} footer={footer}>
        <FiltersPanel
          categories={categories}
          locations={locations}
          attributeDefinitions={attributeDefinitions}
          attributeDefinitionsLoading={attributeDefinitionsLoading}
          attributeDefinitionsError={attributeDefinitionsError}
          values={draftFilters}
          onChange={setDraftFilters}
          onCategoryChange={handleCategoryChange}
          onLocationChange={handleLocationChange}
          onSubmit={applyDraft}
        />
      </FiltersDrawer>
    </div>
  );
}
