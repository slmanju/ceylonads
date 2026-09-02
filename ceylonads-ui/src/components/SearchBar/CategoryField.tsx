import { useEffect, useLayoutEffect, useRef, useState, type CSSProperties } from "react";
import { FaChevronDown, FaChevronRight } from "react-icons/fa";
import type { CategoryResponse } from "../../types/api";
import { childrenOf, findBySlug, rootsOf } from "../../utils/hierarchy";
import { useMediaQuery } from "../../hooks/useMediaQuery";
import { CategorySelector } from "../CategorySelector/CategorySelector";
import { FiltersDrawer } from "../FiltersDrawer/FiltersDrawer";
import "./CategoryField.css";

const MOBILE_QUERY = "(max-width: 860px)";
const FLYOUT_WIDTH = 240;
const FLYOUT_MAX_HEIGHT = 360;
const VIEWPORT_MARGIN = 8;

interface CategoryFieldProps {
  categories: CategoryResponse[];
  value: string;
  onChange: (slug: string) => void;
}

interface CategoryMenuLevelProps {
  categories: CategoryResponse[];
  items: CategoryResponse[];
  value: string;
  onSelect: (slug: string) => void;
  parent?: CategoryResponse;
}

interface CategoryMenuItemProps {
  categories: CategoryResponse[];
  item: CategoryResponse;
  value: string;
  onSelect: (slug: string) => void;
  isActive: boolean;
  onActivate: () => void;
}

function flyoutPositionFor(rowRect: DOMRect): CSSProperties {
  const overflowsRight = rowRect.right + FLYOUT_WIDTH > window.innerWidth - VIEWPORT_MARGIN;
  const left = overflowsRight ? Math.max(VIEWPORT_MARGIN, rowRect.left - FLYOUT_WIDTH) : rowRect.right;
  const top = Math.min(rowRect.top, window.innerHeight - FLYOUT_MAX_HEIGHT - VIEWPORT_MARGIN);

  return { position: "fixed", top: Math.max(VIEWPORT_MARGIN, top), left };
}

function CategoryMenuItem({ categories, item, value, onSelect, isActive, onActivate }: CategoryMenuItemProps) {
  const rowRef = useRef<HTMLButtonElement>(null);
  const [flyoutStyle, setFlyoutStyle] = useState<CSSProperties | null>(null);
  const children = childrenOf(categories, item.id).sort((a, b) => a.displayOrder - b.displayOrder);
  const hasChildren = children.length > 0;

  useLayoutEffect(() => {
    if (isActive && hasChildren && rowRef.current) {
      setFlyoutStyle(flyoutPositionFor(rowRef.current.getBoundingClientRect()));
    }
  }, [isActive, hasChildren]);

  return (
    <li role="none" className="category-field-menu__item" onMouseEnter={onActivate}>
      <button
        ref={rowRef}
        type="button"
        role="menuitem"
        className={`category-field-menu__row ${value === item.slug ? "category-field-menu__row--selected" : ""}`}
        aria-haspopup={hasChildren || undefined}
        aria-expanded={hasChildren ? isActive : undefined}
        onFocus={onActivate}
        onClick={() => (hasChildren ? onActivate() : onSelect(item.slug))}
        title={item.name}
      >
        <span>{item.name}</span>
        {hasChildren && <FaChevronRight aria-hidden="true" />}
      </button>
      {hasChildren && isActive && flyoutStyle && (
        <div className="category-field-menu category-field-menu--flyout" style={flyoutStyle}>
          <CategoryMenuLevel categories={categories} items={children} value={value} onSelect={onSelect} parent={item} />
        </div>
      )}
    </li>
  );
}

function CategoryMenuLevel({ categories, items, value, onSelect, parent }: CategoryMenuLevelProps) {
  const [activeId, setActiveId] = useState<number | null>(null);

  return (
    <ul className="category-field-menu__level" role="menu">
      {parent && (
        <li role="none">
          <button
            type="button"
            role="menuitem"
            className={`category-field-menu__row ${value === parent.slug ? "category-field-menu__row--selected" : ""}`}
            onClick={() => onSelect(parent.slug)}
          >
            All {parent.name}
          </button>
        </li>
      )}
      {items.map((item) => (
        <CategoryMenuItem
          key={item.id}
          categories={categories}
          item={item}
          value={value}
          onSelect={onSelect}
          isActive={activeId === item.id}
          onActivate={() => setActiveId(item.id)}
        />
      ))}
    </ul>
  );
}

export function CategoryField({ categories, value, onChange }: CategoryFieldProps) {
  const isMobile = useMediaQuery(MOBILE_QUERY);
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const selected = value ? findBySlug(categories, value) : undefined;
  const label = selected?.name ?? "All categories";
  const roots = rootsOf(categories).sort((a, b) => a.displayOrder - b.displayOrder);

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
    <div className="category-field" ref={containerRef}>
      <button
        type="button"
        className="category-field__trigger"
        onClick={() => setOpen((prev) => !prev)}
        aria-haspopup="true"
        aria-expanded={open}
        aria-label={`Category: ${label}`}
        title={label}
      >
        <span className="category-field__label">{label}</span>
        <FaChevronDown aria-hidden="true" className="category-field__chevron" />
      </button>

      {open && !isMobile && (
        <div className="category-field-menu" role="menu">
          <button
            type="button"
            role="menuitem"
            className={`category-field-menu__row ${value === "" ? "category-field-menu__row--selected" : ""}`}
            onClick={() => handleSelect("")}
          >
            All categories
          </button>
          <CategoryMenuLevel categories={categories} items={roots} value={value} onSelect={handleSelect} />
        </div>
      )}

      {isMobile && (
        <FiltersDrawer open={open} onClose={() => setOpen(false)} title="Category">
          <CategorySelector
            categories={categories}
            value={value}
            onSelect={handleSelect}
            onClose={() => setOpen(false)}
            rootBackLabel="Close"
          />
        </FiltersDrawer>
      )}
    </div>
  );
}
