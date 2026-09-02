import { FaTimes } from "react-icons/fa";
import type { LocationResponse, TuitionFilterMetadataResponse } from "../../types/api";
import type { ClassFilterValues } from "../../tuition/model/searchFilters";
import { CLASS_FORMAT_LABELS, CLASS_PURPOSE_LABELS } from "../../tuition/model/labels";
import { findBySlug } from "../../utils/hierarchy";
import { formatPrice } from "../../utils/formatPrice";
import "./ActiveFilterChips.css";

interface Chip {
  key: string;
  label: string;
  onRemove: () => void;
}

interface ActiveFilterChipsProps {
  values: ClassFilterValues;
  filters: TuitionFilterMetadataResponse | null;
  locations: LocationResponse[];
  fixedLocationSlug?: string;
  onChange: (patch: Partial<ClassFilterValues>) => void;
  onClearAll: () => void;
}

function optionLabel(options: { value: string; label: string }[] | undefined, value: string): string {
  return options?.find((o) => o.value === value)?.label ?? value;
}

// Renders the current filter state (minus any category/location locked by the page itself) as
// removable chips - the same `values` object the top filter bar and mobile drawer read and write,
// so removing a chip immediately updates the shared state and re-runs the search.
export function ActiveFilterChips({
  values,
  filters,
  locations,
  fixedLocationSlug,
  onChange,
  onClearAll,
}: ActiveFilterChipsProps) {
  const chips: Chip[] = [];

  if (values.subject) {
    chips.push({ key: "subject", label: optionLabel(filters?.subjects, values.subject), onRemove: () => onChange({ subject: "" }) });
  }
  if (values.level) {
    chips.push({ key: "level", label: optionLabel(filters?.levels, values.level), onRemove: () => onChange({ level: "" }) });
  }
  if (values.curriculum) {
    chips.push({
      key: "curriculum",
      label: optionLabel(filters?.curricula, values.curriculum),
      onRemove: () => onChange({ curriculum: "" }),
    });
  }
  if (values.medium) {
    chips.push({ key: "medium", label: optionLabel(filters?.mediums, values.medium), onRemove: () => onChange({ medium: "" }) });
  }
  if (values.deliveryMode) {
    chips.push({
      key: "deliveryMode",
      label: optionLabel(filters?.deliveryModes, values.deliveryMode),
      onRemove: () => onChange({ deliveryMode: "" }),
    });
  }
  if (!fixedLocationSlug && values.location) {
    chips.push({
      key: "location",
      label: findBySlug(locations, values.location)?.name ?? values.location,
      onRemove: () => onChange({ location: "" }),
    });
  }
  if (values.minPrice || values.maxPrice) {
    const label =
      values.minPrice && values.maxPrice
        ? `${formatPrice(Number(values.minPrice))} – ${formatPrice(Number(values.maxPrice))}`
        : values.minPrice
          ? `From ${formatPrice(Number(values.minPrice))}`
          : `Up to ${formatPrice(Number(values.maxPrice))}`;
    chips.push({ key: "fee", label, onRemove: () => onChange({ minPrice: "", maxPrice: "" }) });
  }
  values.classFormats.forEach((format) => {
    chips.push({
      key: `classFormat-${format}`,
      label: CLASS_FORMAT_LABELS[format],
      onRemove: () => onChange({ classFormats: values.classFormats.filter((f) => f !== format) }),
    });
  });
  values.classPurposes.forEach((purpose) => {
    chips.push({
      key: `classPurpose-${purpose}`,
      label: CLASS_PURPOSE_LABELS[purpose],
      onRemove: () => onChange({ classPurposes: values.classPurposes.filter((p) => p !== purpose) }),
    });
  });
  if (chips.length === 0) return null;

  return (
    <div className="active-filter-chips">
      {chips.map((chip) => (
        <button type="button" key={chip.key} className="active-filter-chips__chip" onClick={chip.onRemove}>
          {chip.label}
          <FaTimes aria-hidden="true" />
        </button>
      ))}
      <button type="button" className="active-filter-chips__clear" onClick={onClearAll}>
        Clear all
      </button>
    </div>
  );
}
