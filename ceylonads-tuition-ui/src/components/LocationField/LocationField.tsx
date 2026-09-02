import { useEffect, useRef, useState } from "react";
import { FaChevronDown } from "react-icons/fa";
import type { LocationResponse } from "../../types/api";
import { findBySlug } from "../../utils/hierarchy";
import { useMediaQuery } from "../../hooks/useMediaQuery";
import { LocationSelector } from "../LocationSelector/LocationSelector";
import { FiltersDrawer } from "../FiltersDrawer/FiltersDrawer";
import "./LocationField.css";

const MOBILE_QUERY = "(max-width: 860px)";

interface LocationFieldProps {
  locations: LocationResponse[];
  value: string;
  onChange: (slug: string) => void;
}

export function LocationField({ locations, value, onChange }: LocationFieldProps) {
  const isMobile = useMediaQuery(MOBILE_QUERY);
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const selected = value ? findBySlug(locations, value) : undefined;
  const label = selected?.name ?? "All Sri Lanka";

  useEffect(() => {
    if (!open || isMobile) return;

    const handlePointerDown = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open, isMobile]);

  const handleSelect = (slug: string) => {
    onChange(slug);
    setOpen(false);
  };

  return (
    <div className="location-field" ref={containerRef}>
      <button
        type="button"
        className="location-field__trigger"
        onClick={() => setOpen((prev) => !prev)}
        aria-haspopup="true"
        aria-expanded={open}
        aria-label={`Location: ${label}`}
        title={label}
      >
        <span className="location-field__label">{label}</span>
        <FaChevronDown aria-hidden="true" className="location-field__chevron" />
      </button>

      {open && !isMobile && (
        <div className="location-field__popover">
          <LocationSelector locations={locations} value={value} onSelect={handleSelect} onClose={() => setOpen(false)} />
        </div>
      )}

      {isMobile && (
        <FiltersDrawer open={open} onClose={() => setOpen(false)} title="Location">
          <LocationSelector locations={locations} value={value} onSelect={handleSelect} onClose={() => setOpen(false)} />
        </FiltersDrawer>
      )}
    </div>
  );
}
