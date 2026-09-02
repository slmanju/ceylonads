import { useState } from "react";
import { FaChevronLeft, FaChevronRight } from "react-icons/fa";
import type { CategoryResponse } from "../../types/api";
import { childrenOf, hasChildren, rootsOf } from "../../utils/hierarchy";
import "./CategorySelector.css";

interface CategorySelectorProps {
  categories: CategoryResponse[];
  value: string;
  onSelect: (slug: string) => void;
  onClose: () => void;
  /** Label for the top-level back button, which calls onClose. Defaults to the filters-panel wording. */
  rootBackLabel?: string;
  /**
   * Seed the drill-down at this category instead of the true top-level category list, e.g. when the
   * page has already locked in a top-level category via its route (/category/vehicles). Back from
   * this seeded level closes the selector rather than revealing unrelated top-level categories.
   */
  startCategory?: CategoryResponse;
}

export function CategorySelector({
  categories,
  value,
  onSelect,
  onClose,
  rootBackLabel = "Back to filters",
  startCategory,
}: CategorySelectorProps) {
  // startCategory only seeds where the picker opens (e.g. straight into Vehicles' children on a
  // /category/vehicles page) - it is not a floor. Back always keeps walking up through every real
  // ancestor level, through the true top-level category list, before finally closing the picker.
  const [navStack, setNavStack] = useState<CategoryResponse[]>(startCategory ? [startCategory] : []);
  const current = navStack[navStack.length - 1];
  const parent = navStack[navStack.length - 2];
  const list = current
    ? childrenOf(categories, current.id).sort((a, b) => a.displayOrder - b.displayOrder)
    : rootsOf(categories).sort((a, b) => a.displayOrder - b.displayOrder);

  const handleRowClick = (category: CategoryResponse) => {
    if (hasChildren(categories, category.id)) {
      setNavStack([...navStack, category]);
    } else {
      onSelect(category.slug);
    }
  };

  const handleBack = () => {
    if (navStack.length === 0) {
      onClose();
    } else {
      setNavStack(navStack.slice(0, -1));
    }
  };

  const backLabel = navStack.length === 0 ? rootBackLabel : (parent ? parent.name : "All Categories");

  return (
    <div className="category-selector">
      <button type="button" className="category-selector__back" onClick={handleBack}>
        <FaChevronLeft aria-hidden="true" />
        {backLabel}
      </button>

      {current && <h3 className="category-selector__title">{current.name}</h3>}

      <ul className="category-selector__list">
        <li>
          <button
            type="button"
            className={`category-selector__row ${value === (current?.slug ?? "") ? "category-selector__row--selected" : ""}`}
            onClick={() => onSelect(current?.slug ?? "")}
          >
            {current ? `All ${current.name}` : "All Categories"}
          </button>
        </li>
        {list.map((category) => (
          <li key={category.id}>
            <button
              type="button"
              className={`category-selector__row ${value === category.slug ? "category-selector__row--selected" : ""}`}
              onClick={() => handleRowClick(category)}
            >
              <span>{category.name}</span>
              {hasChildren(categories, category.id) && <FaChevronRight aria-hidden="true" />}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
