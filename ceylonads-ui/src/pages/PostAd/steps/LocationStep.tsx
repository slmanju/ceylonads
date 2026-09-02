import { useState } from "react";
import { FaChevronDown, FaChevronRight, FaMapMarkerAlt, FaSearch, FaTimes } from "react-icons/fa";
import type { CategoryResponse, LocationResponse } from "../../../types/api";
import { LoadingState } from "../../../components/LoadingState/LoadingState";
import { ErrorState } from "../../../components/ErrorState/ErrorState";
import { childrenOf, hasChildren, rootsOf } from "../../../utils/hierarchy";
import { categoryAncestors } from "../../../utils/categoryHierarchy";
import "./LocationStep.css";

interface LocationStepProps {
  locations: LocationResponse[];
  loading: boolean;
  error: string | null;
  locationSlugs: string[];
  onChange: (locationSlugs: string[]) => void;
  categories: CategoryResponse[];
  categorySlug: string;
  attributeValues: Record<string, string>;
}

// Category-dependent copy only - the persistence/business model is identical for every category.
function stepCopy(categories: CategoryResponse[], categorySlug: string, attributeValues: Record<string, string>) {
  const category = categories.find((c) => c.slug === categorySlug);
  const rootSlug = category ? categoryAncestors(categories, category)[0]?.slug : undefined;

  if (rootSlug === "education-tuition") {
    const online = attributeValues.classMode === "ONLINE";
    return {
      title: "Where do you teach?",
      subtitle: online
        ? "Class Mode is set to Online, so no physical location is needed."
        : "Add every town or area you teach in - buyers can find you by any of them.",
    };
  }
  if (rootSlug === "services") {
    return {
      title: "Service areas",
      subtitle: "Add the towns or areas you serve. Leave empty for a fully remote/islandwide service.",
    };
  }
  return {
    title: "Location",
    subtitle: "Search for a city or area, or browse the list below.",
  };
}

export function LocationStep({
  locations,
  loading,
  error,
  locationSlugs,
  onChange,
  categories,
  categorySlug,
  attributeValues,
}: LocationStepProps) {
  const [query, setQuery] = useState("");
  const [browseOpen, setBrowseOpen] = useState(false);
  const [navStack, setNavStack] = useState<LocationResponse[]>([]);

  if (loading) return <LoadingState label="Loading locations…" />;
  if (error) return <ErrorState message={error} />;

  const selected = locations.filter((l) => locationSlugs.includes(l.slug));

  const add = (slug: string) => {
    if (!locationSlugs.includes(slug)) onChange([...locationSlugs, slug]);
    setQuery("");
  };
  const remove = (slug: string) => onChange(locationSlugs.filter((s) => s !== slug));

  const searchResults =
    query.trim().length > 0
      ? locations
          .filter((l) => l.name.toLowerCase().includes(query.trim().toLowerCase()) && !locationSlugs.includes(l.slug))
          .sort((a, b) => a.name.localeCompare(b.name))
          .slice(0, 20)
      : [];

  const current = navStack[navStack.length - 1];
  const browseList = (current ? childrenOf(locations, current.id) : rootsOf(locations))
    .slice()
    .sort((a, b) => a.name.localeCompare(b.name));

  const handleBrowseRowClick = (location: LocationResponse) => {
    if (hasChildren(locations, location.id)) {
      setNavStack([...navStack, location]);
    } else {
      add(location.slug);
    }
  };

  const copy = stepCopy(categories, categorySlug, attributeValues);

  return (
    <div className="post-ad-step">
      <h2 className="post-ad-step__title">{copy.title}</h2>
      <p className="post-ad-step__subtitle">{copy.subtitle}</p>

      <div className="location-step">
        <div className="location-step__search">
          <FaSearch aria-hidden="true" />
          <label htmlFor="location-step-search" className="visually-hidden">
            Search city or area
          </label>
          <input
            id="location-step-search"
            type="text"
            placeholder="Search city or area…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>

        {searchResults.length > 0 && (
          <ul className="location-step__results">
            {searchResults.map((location) => (
              <li key={location.id}>
                <button type="button" className="location-step__result" onClick={() => add(location.slug)}>
                  <FaMapMarkerAlt aria-hidden="true" />
                  {location.name}
                </button>
              </li>
            ))}
          </ul>
        )}
        {query.trim().length > 0 && searchResults.length === 0 && (
          <p className="location-step__no-results">No locations match "{query}"</p>
        )}

        {selected.length > 0 && (
          <div className="location-step__selected">
            <span className="location-step__selected-label">Selected locations</span>
            <div className="location-step__chips">
              {selected.map((location) => (
                <button
                  key={location.id}
                  type="button"
                  className="location-step__chip"
                  onClick={() => remove(location.slug)}
                >
                  {location.name}
                  <FaTimes aria-hidden="true" />
                </button>
              ))}
            </div>
          </div>
        )}

        <button
          type="button"
          className="location-step__browse-toggle"
          onClick={() => setBrowseOpen((v) => !v)}
        >
          {browseOpen ? <FaChevronDown aria-hidden="true" /> : <FaChevronRight aria-hidden="true" />}
          Browse locations
        </button>

        {browseOpen && (
          <div className="location-step__browse">
            <button
              type="button"
              className="location-step__browse-back"
              onClick={() => setNavStack(navStack.slice(0, -1))}
              disabled={navStack.length === 0}
            >
              <FaChevronRight aria-hidden="true" style={{ transform: "rotate(180deg)" }} />
              {current ? "Back" : "Top level"}
            </button>
            <ul className="location-step__browse-list">
              {browseList.map((location) => (
                <li key={location.id}>
                  <button
                    type="button"
                    className={`location-step__browse-row ${locationSlugs.includes(location.slug) ? "location-step__browse-row--selected" : ""}`}
                    onClick={() => handleBrowseRowClick(location)}
                  >
                    <span>{location.name}</span>
                    {hasChildren(locations, location.id) ? (
                      <FaChevronRight aria-hidden="true" />
                    ) : (
                      locationSlugs.includes(location.slug) && <FaTimes aria-hidden="true" />
                    )}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
