import { useState } from "react";
import { FaChevronRight } from "react-icons/fa";
import type { AttributeDefinitionResponse, CategoryResponse, LocationResponse } from "../../types/api";
import { AttributeField } from "../AttributeFields/AttributeField";
import { CategorySelector } from "../CategorySelector/CategorySelector";
import { LocationSelector } from "../LocationSelector/LocationSelector";
import { findBySlug } from "../../utils/hierarchy";
import { categoryAncestors } from "../../utils/categoryHierarchy";
import "./FiltersPanel.css";

export interface FilterValues {
  q: string;
  category: string;
  location: string;
  minPrice: string;
  maxPrice: string;
  // Range-capable attributes store "<key>.min" / "<key>.max" as separate entries.
  attributes: Record<string, string>;
}

type PanelView = "root" | "category" | "location";

interface FiltersPanelProps {
  categories: CategoryResponse[];
  locations: LocationResponse[];
  attributeDefinitions?: AttributeDefinitionResponse[];
  attributeDefinitionsLoading?: boolean;
  attributeDefinitionsError?: string | null;
  values: FilterValues;
  onChange: (values: FilterValues) => void;
  onCategoryChange: (slug: string) => void;
  onLocationChange: (slug: string) => void;
  onSubmit?: () => void;
}

export function FiltersPanel({
  categories,
  locations,
  attributeDefinitions = [],
  attributeDefinitionsLoading = false,
  attributeDefinitionsError = null,
  values,
  onChange,
  onCategoryChange,
  onLocationChange,
  onSubmit,
}: FiltersPanelProps) {
  const [view, setView] = useState<PanelView>("root");

  const selectedCategory = values.category ? findBySlug(categories, values.category) : undefined;
  // Opens the picker scoped to the currently selected category's top-level branch, so switching
  // between siblings (e.g. Cars -> Motorcycles) doesn't require walking back to the root each time;
  // the picker's own back navigation still walks all the way up to the true root category list.
  const startCategory = selectedCategory ? categoryAncestors(categories, selectedCategory)[0] : undefined;

  const categoryLabel = selectedCategory?.name ?? "All Categories";
  const locationLabel = values.location ? (findBySlug(locations, values.location)?.name ?? values.location) : "All Sri Lanka";

  const minPrice = values.minPrice ? Number(values.minPrice) : null;
  const maxPrice = values.maxPrice ? Number(values.maxPrice) : null;
  const priceRangeInvalid = minPrice !== null && maxPrice !== null && minPrice > maxPrice;

  if (view === "category") {
    return (
      <CategorySelector
        categories={categories}
        value={values.category}
        startCategory={startCategory}
        onSelect={(slug) => {
          onCategoryChange(slug);
          setView("root");
        }}
        onClose={() => setView("root")}
      />
    );
  }

  if (view === "location") {
    return (
      <LocationSelector
        locations={locations}
        value={values.location}
        onSelect={(slug) => {
          onLocationChange(slug);
          setView("root");
        }}
        onClose={() => setView("root")}
      />
    );
  }

  return (
    <div className="filters-panel">
      <div className="filters-panel__group">
        <label htmlFor="filter-search-q">Search</label>
        <input
          id="filter-search-q"
          type="text"
          placeholder="Search ads..."
          value={values.q}
          onChange={(e) => onChange({ ...values, q: e.target.value })}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              onSubmit?.();
            }
          }}
        />
      </div>

      <div className="filters-panel__group">
        <span className="filters-panel__label">Category</span>
        <button type="button" className="filters-panel__picker-row" onClick={() => setView("category")}>
          <span>{categoryLabel}</span>
          <FaChevronRight aria-hidden="true" />
        </button>
      </div>

      <div className="filters-panel__group">
        <span className="filters-panel__label">Location</span>
        <button type="button" className="filters-panel__picker-row" onClick={() => setView("location")}>
          <span>{locationLabel}</span>
          <FaChevronRight aria-hidden="true" />
        </button>
      </div>

      <div className="filters-panel__group">
        <label htmlFor="filter-min-price">Price (Rs.)</label>
        <div className="filters-panel__price-row">
          <input
            id="filter-min-price"
            type="number"
            min={0}
            placeholder="Min"
            value={values.minPrice}
            onChange={(e) => onChange({ ...values, minPrice: e.target.value })}
          />
          <span aria-hidden="true">–</span>
          <label htmlFor="filter-max-price" className="visually-hidden">
            Maximum price
          </label>
          <input
            id="filter-max-price"
            type="number"
            min={0}
            placeholder="Max"
            value={values.maxPrice}
            onChange={(e) => onChange({ ...values, maxPrice: e.target.value })}
          />
        </div>
        {priceRangeInvalid && <span className="filters-panel__error">Min price is higher than max price.</span>}
      </div>

      {attributeDefinitionsLoading && <p className="filters-panel__hint">Loading filters…</p>}
      {attributeDefinitionsError && <p className="filters-panel__error">{attributeDefinitionsError}</p>}

      {attributeDefinitions.map((definition) => (
        <div className="filters-panel__group" key={definition.id}>
          <AttributeField
            definition={definition}
            mode="filter"
            values={values.attributes}
            onChange={(key, value) => onChange({ ...values, attributes: { ...values.attributes, [key]: value } })}
          />
        </div>
      ))}
    </div>
  );
}
