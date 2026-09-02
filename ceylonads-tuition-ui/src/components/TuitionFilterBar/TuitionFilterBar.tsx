import { useRef, useState } from "react";
import { FaChevronDown, FaSlidersH } from "react-icons/fa";
import type { LocationResponse, TuitionFilterMetadataResponse } from "../../types/api";
import type { ClassFormat, ClassPurpose } from "../../tuition/model/tuition";
import type { ClassFilterValues } from "../../tuition/model/searchFilters";
import { SubjectCombobox } from "../SubjectCombobox/SubjectCombobox";
import { LocationSelector } from "../LocationSelector/LocationSelector";
import { FeeRangeFields } from "../SubjectFilters/FeeRangeFields";
import { AdvancedFilterFields } from "../SubjectFilters/AdvancedFilterFields";
import { useClickOutside } from "../../hooks/useClickOutside";
import { findBySlug } from "../../utils/hierarchy";
import { formatPrice } from "../../utils/formatPrice";
import "./TuitionFilterBar.css";

interface TuitionFilterBarProps {
  filters: TuitionFilterMetadataResponse | null;
  filtersLoading: boolean;
  locations: LocationResponse[];
  showLocationPicker: boolean;
  values: ClassFilterValues;
  onChange: (patch: Partial<ClassFilterValues>) => void;
}

function SelectField({
  id,
  label,
  value,
  onChange,
  options,
  anyLabel,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: { value: string; label: string }[];
  anyLabel: string;
}) {
  return (
    <div className="tuition-filter-bar__field">
      <label htmlFor={id}>{label}</label>
      <select id={id} value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">{anyLabel}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  );
}

export function TuitionFilterBar({
  filters,
  filtersLoading,
  locations,
  showLocationPicker,
  values,
  onChange,
}: TuitionFilterBarProps) {
  const [locationOpen, setLocationOpen] = useState(false);
  const [feeOpen, setFeeOpen] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  const [feeDraft, setFeeDraft] = useState({ minPrice: values.minPrice, maxPrice: values.maxPrice });
  const [moreDraft, setMoreDraft] = useState<{ classFormats: ClassFormat[]; classPurposes: ClassPurpose[] }>({
    classFormats: values.classFormats,
    classPurposes: values.classPurposes,
  });

  const locationRef = useRef<HTMLDivElement>(null);
  const feeRef = useRef<HTMLDivElement>(null);
  const moreRef = useRef<HTMLDivElement>(null);

  useClickOutside(locationRef, () => setLocationOpen(false), locationOpen);
  useClickOutside(feeRef, () => setFeeOpen(false), feeOpen);
  useClickOutside(moreRef, () => setMoreOpen(false), moreOpen);

  const locationLabel = values.location ? (findBySlug(locations, values.location)?.name ?? values.location) : null;

  const feeLabel =
    values.minPrice && values.maxPrice
      ? `${formatPrice(Number(values.minPrice))} – ${formatPrice(Number(values.maxPrice))}`
      : values.minPrice
        ? `From ${formatPrice(Number(values.minPrice))}`
        : values.maxPrice
          ? `Up to ${formatPrice(Number(values.maxPrice))}`
          : null;

  const advancedCount = values.classFormats.length + values.classPurposes.length;

  const openFeePopover = () => {
    setFeeDraft({ minPrice: values.minPrice, maxPrice: values.maxPrice });
    setFeeOpen(true);
  };

  const applyFee = () => {
    onChange({ minPrice: feeDraft.minPrice, maxPrice: feeDraft.maxPrice });
    setFeeOpen(false);
  };

  const openMorePopover = () => {
    setMoreDraft({ classFormats: values.classFormats, classPurposes: values.classPurposes });
    setMoreOpen(true);
  };

  const applyMore = () => {
    onChange({ classFormats: moreDraft.classFormats, classPurposes: moreDraft.classPurposes });
    setMoreOpen(false);
  };

  return (
    <div className="tuition-filter-bar">
      <div className="tuition-filter-bar__field tuition-filter-bar__field--combobox">
        <label htmlFor="filter-subject">Subject</label>
        <SubjectCombobox
          id="filter-subject"
          label="Subject"
          options={filters?.subjects ?? []}
          value={values.subject}
          onChange={(subject) => onChange({ subject })}
          loading={filtersLoading}
        />
      </div>

      <SelectField
        id="filter-level"
        label="Grade / Level"
        value={values.level}
        onChange={(level) => onChange({ level })}
        options={filters?.levels ?? []}
        anyLabel="Any level"
      />

      <SelectField
        id="filter-curriculum"
        label="Curriculum"
        value={values.curriculum}
        onChange={(curriculum) => onChange({ curriculum })}
        options={filters?.curricula ?? []}
        anyLabel="Any curriculum"
      />

      <SelectField
        id="filter-medium"
        label="Medium"
        value={values.medium}
        onChange={(medium) => onChange({ medium })}
        options={filters?.mediums ?? []}
        anyLabel="Any medium"
      />

      <SelectField
        id="filter-delivery-mode"
        label="Delivery Mode"
        value={values.deliveryMode}
        onChange={(deliveryMode) => onChange({ deliveryMode })}
        options={filters?.deliveryModes ?? []}
        anyLabel="Any delivery mode"
      />

      {showLocationPicker && (
        <div className="tuition-filter-bar__popover" ref={locationRef}>
          <button
            type="button"
            className="tuition-filter-bar__popover-trigger"
            aria-expanded={locationOpen}
            onClick={() => setLocationOpen((open) => !open)}
          >
            <span className="tuition-filter-bar__popover-text">
              <span className="tuition-filter-bar__popover-caption">Location</span>
              <span className="tuition-filter-bar__popover-value">{locationLabel ?? "All Sri Lanka"}</span>
            </span>
            <FaChevronDown aria-hidden="true" />
          </button>
          {locationOpen && (
            <div className="tuition-filter-bar__panel tuition-filter-bar__panel--location">
              <LocationSelector
                locations={locations}
                value={values.location}
                onSelect={(slug) => {
                  onChange({ location: slug });
                  setLocationOpen(false);
                }}
                onClose={() => setLocationOpen(false)}
              />
            </div>
          )}
        </div>
      )}

      <div className="tuition-filter-bar__popover" ref={feeRef}>
        <button
          type="button"
          className="tuition-filter-bar__popover-trigger"
          aria-expanded={feeOpen}
          onClick={() => (feeOpen ? setFeeOpen(false) : openFeePopover())}
        >
          <span className="tuition-filter-bar__popover-text">
            <span className="tuition-filter-bar__popover-caption">Fee</span>
            <span className="tuition-filter-bar__popover-value">{feeLabel ?? "Any"}</span>
          </span>
          <FaChevronDown aria-hidden="true" />
        </button>
        {feeOpen && (
          <div className="tuition-filter-bar__panel tuition-filter-bar__panel--fee">
            <FeeRangeFields
              idPrefix="bar-fee"
              minPrice={feeDraft.minPrice}
              maxPrice={feeDraft.maxPrice}
              onChange={(minPrice, maxPrice) => setFeeDraft({ minPrice, maxPrice })}
            />
            <div className="tuition-filter-bar__panel-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setFeeDraft({ minPrice: "", maxPrice: "" });
                }}
              >
                Reset
              </button>
              <button type="button" className="btn btn-accent" onClick={applyFee}>
                Apply
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="tuition-filter-bar__popover" ref={moreRef}>
        <button
          type="button"
          className="tuition-filter-bar__popover-trigger tuition-filter-bar__more-trigger"
          aria-expanded={moreOpen}
          onClick={() => (moreOpen ? setMoreOpen(false) : openMorePopover())}
        >
          <FaSlidersH aria-hidden="true" />
          <span>More Filters</span>
          {advancedCount > 0 && <span className="tuition-filter-bar__more-badge">{advancedCount}</span>}
        </button>
        {moreOpen && (
          <div className="tuition-filter-bar__panel tuition-filter-bar__panel--more">
            <AdvancedFilterFields
              classFormats={moreDraft.classFormats}
              classPurposes={moreDraft.classPurposes}
              onChange={(classFormats, classPurposes) => setMoreDraft({ classFormats, classPurposes })}
            />
            <div className="tuition-filter-bar__panel-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setMoreDraft({ classFormats: [], classPurposes: [] })}>
                Reset
              </button>
              <button type="button" className="btn btn-accent" onClick={applyMore}>
                Apply Filters
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
