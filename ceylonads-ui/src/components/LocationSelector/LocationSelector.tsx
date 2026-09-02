import { useState } from "react";
import { FaChevronLeft, FaChevronRight, FaSearch } from "react-icons/fa";
import type { LocationResponse } from "../../types/api";
import { childrenOf, hasChildren, rootsOf } from "../../utils/hierarchy";
import "./LocationSelector.css";

interface LocationSelectorProps {
  locations: LocationResponse[];
  value: string;
  onSelect: (slug: string) => void;
  onClose: () => void;
}

export function LocationSelector({ locations, value, onSelect, onClose }: LocationSelectorProps) {
  const [navStack, setNavStack] = useState<LocationResponse[]>([]);
  const [query, setQuery] = useState("");
  const current = navStack[navStack.length - 1];

  const searching = query.trim().length > 0;
  const searchResults = searching
    ? locations
        .filter((l) => l.name.toLowerCase().includes(query.trim().toLowerCase()))
        .sort((a, b) => a.name.localeCompare(b.name))
        .slice(0, 50)
    : [];

  const list = current
    ? childrenOf(locations, current.id).sort((a, b) => a.name.localeCompare(b.name))
    : rootsOf(locations).sort((a, b) => a.name.localeCompare(b.name));

  const handleRowClick = (location: LocationResponse) => {
    if (hasChildren(locations, location.id)) {
      setNavStack([...navStack, location]);
    } else {
      onSelect(location.slug);
    }
  };

  const handleBack = () => {
    if (navStack.length === 0) {
      onClose();
    } else {
      setNavStack(navStack.slice(0, -1));
    }
  };

  return (
    <div className="location-selector">
      <button type="button" className="location-selector__back" onClick={handleBack}>
        <FaChevronLeft aria-hidden="true" />
        {current ? "Back" : "Back to filters"}
      </button>

      <h3 className="location-selector__title">Select Location</h3>

      <div className="location-selector__search">
        <FaSearch aria-hidden="true" />
        <label htmlFor="location-search" className="visually-hidden">
          Search location
        </label>
        <input
          id="location-search"
          type="text"
          placeholder="Search location..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      {searching ? (
        <ul className="location-selector__list">
          {searchResults.length === 0 && <li className="location-selector__empty">No locations match "{query}"</li>}
          {searchResults.map((location) => (
            <li key={location.id}>
              <button
                type="button"
                className={`location-selector__row ${value === location.slug ? "location-selector__row--selected" : ""}`}
                onClick={() => onSelect(location.slug)}
              >
                {location.name}
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <ul className="location-selector__list">
          <li>
            <button
              type="button"
              className={`location-selector__row ${value === (current?.slug ?? "") ? "location-selector__row--selected" : ""}`}
              onClick={() => onSelect(current?.slug ?? "")}
            >
              {current ? `All ${current.name}` : "All Sri Lanka"}
            </button>
          </li>
          {list.map((location) => (
            <li key={location.id}>
              <button
                type="button"
                className={`location-selector__row ${value === location.slug ? "location-selector__row--selected" : ""}`}
                onClick={() => handleRowClick(location)}
              >
                <span>{location.name}</span>
                {hasChildren(locations, location.id) && <FaChevronRight aria-hidden="true" />}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
