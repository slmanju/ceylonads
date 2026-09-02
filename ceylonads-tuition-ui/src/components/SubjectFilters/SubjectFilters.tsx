import { useState } from "react";
import { FaChevronDown, FaChevronRight } from "react-icons/fa";
import type { LocationResponse, TuitionFilterMetadataResponse } from "../../types/api";
import type { ClassFilterValues } from "../../tuition/model/searchFilters";
import { SubjectCombobox } from "../SubjectCombobox/SubjectCombobox";
import { LocationSelector } from "../LocationSelector/LocationSelector";
import { FeeRangeFields } from "./FeeRangeFields";
import { AdvancedFilterFields } from "./AdvancedFilterFields";
import { findBySlug } from "../../utils/hierarchy";
import "./SubjectFilters.css";

type PanelView = "root" | "location";

interface SubjectFiltersProps {
  locations: LocationResponse[];
  filters: TuitionFilterMetadataResponse | null;
  filtersLoading?: boolean;
  showLocationPicker?: boolean;
  values: ClassFilterValues;
  onChange: (values: ClassFilterValues) => void;
}

// Vertical, stacked rendering of the same tuition filter fields as TuitionFilterBar - used inside
// the mobile filter drawer, where a single full-height panel reads better than a horizontal
// toolbar. Both components read and write the same ClassFilterValues, so desktop/mobile never
// hold separate filter state.
export function SubjectFilters({
  locations,
  filters,
  filtersLoading = false,
  showLocationPicker = true,
  values,
  onChange,
}: SubjectFiltersProps) {
  const [view, setView] = useState<PanelView>("root");
  const [advancedOpen, setAdvancedOpen] = useState(
    () => values.classFormats.length > 0 || values.classPurposes.length > 0,
  );
  const advancedCount = values.classFormats.length + values.classPurposes.length;

  const locationLabel = values.location ? (findBySlug(locations, values.location)?.name ?? values.location) : "All Sri Lanka";

  if (view === "location") {
    return (
      <LocationSelector
        locations={locations}
        value={values.location}
        onSelect={(slug) => {
          onChange({ ...values, location: slug });
          setView("root");
        }}
        onClose={() => setView("root")}
      />
    );
  }

  return (
    <div className="subject-filters">
      <div className="subject-filters__group">
        <label htmlFor="drawer-filter-subject">Subject</label>
        <SubjectCombobox
          id="drawer-filter-subject"
          label="Subject"
          options={filters?.subjects ?? []}
          value={values.subject}
          onChange={(subject) => onChange({ ...values, subject })}
          loading={filtersLoading}
        />
      </div>

      <div className="subject-filters__group">
        <label htmlFor="drawer-filter-level">Grade / Level</label>
        <select id="drawer-filter-level" value={values.level} onChange={(e) => onChange({ ...values, level: e.target.value })}>
          <option value="">Any level</option>
          {(filters?.levels ?? []).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="subject-filters__group">
        <label htmlFor="drawer-filter-curriculum">Curriculum</label>
        <select
          id="drawer-filter-curriculum"
          value={values.curriculum}
          onChange={(e) => onChange({ ...values, curriculum: e.target.value })}
        >
          <option value="">Any curriculum</option>
          {(filters?.curricula ?? []).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="subject-filters__group">
        <label htmlFor="drawer-filter-medium">Medium</label>
        <select id="drawer-filter-medium" value={values.medium} onChange={(e) => onChange({ ...values, medium: e.target.value })}>
          <option value="">Any medium</option>
          {(filters?.mediums ?? []).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="subject-filters__group">
        <label htmlFor="drawer-filter-delivery-mode">Delivery Mode</label>
        <select
          id="drawer-filter-delivery-mode"
          value={values.deliveryMode}
          onChange={(e) => onChange({ ...values, deliveryMode: e.target.value })}
        >
          <option value="">Any delivery mode</option>
          {(filters?.deliveryModes ?? []).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {showLocationPicker && (
        <div className="subject-filters__group">
          <span className="subject-filters__label">Location</span>
          <button type="button" className="subject-filters__picker-row" onClick={() => setView("location")}>
            <span>{locationLabel}</span>
            <FaChevronRight aria-hidden="true" />
          </button>
        </div>
      )}

      <FeeRangeFields
        idPrefix="drawer-filter"
        minPrice={values.minPrice}
        maxPrice={values.maxPrice}
        onChange={(minPrice, maxPrice) => onChange({ ...values, minPrice, maxPrice })}
      />

      <div className="subject-filters__advanced">
        <button
          type="button"
          className="subject-filters__advanced-toggle"
          aria-expanded={advancedOpen}
          onClick={() => setAdvancedOpen((prev) => !prev)}
        >
          <span>
            More filters
            {advancedCount > 0 && <span className="subject-filters__advanced-badge">{advancedCount}</span>}
          </span>
          <FaChevronDown aria-hidden="true" className={`subject-filters__advanced-chevron${advancedOpen ? " subject-filters__advanced-chevron--open" : ""}`} />
        </button>

        {advancedOpen && (
          <div className="subject-filters__advanced-body">
            <AdvancedFilterFields
              classFormats={values.classFormats}
              classPurposes={values.classPurposes}
              onChange={(classFormats, classPurposes) => onChange({ ...values, classFormats, classPurposes })}
            />
          </div>
        )}
      </div>
    </div>
  );
}
