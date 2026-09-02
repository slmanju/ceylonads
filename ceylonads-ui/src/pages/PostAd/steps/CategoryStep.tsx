import { useState } from "react";
import { FaCheckCircle, FaChevronRight, FaSearch } from "react-icons/fa";
import type { CategoryResponse } from "../../../types/api";
import { getCategoryIcon } from "../../../utils/categoryIcons";
import { LoadingState } from "../../../components/LoadingState/LoadingState";
import { ErrorState } from "../../../components/ErrorState/ErrorState";
import { childrenOf, hasChildren, rootsOf } from "../../../utils/hierarchy";
import { categoryAncestors } from "../../../utils/categoryHierarchy";

interface CategoryStepProps {
  categories: CategoryResponse[];
  loading: boolean;
  error: string | null;
  categorySlug: string;
  onSelect: (slug: string, path: string) => void;
}

function pathFor(categories: CategoryResponse[], category: CategoryResponse): string {
  return categoryAncestors(categories, category)
    .map((c) => c.name)
    .join(" › ");
}

export function CategoryStep({ categories, loading, error, categorySlug, onSelect }: CategoryStepProps) {
  const selected = categorySlug ? categories.find((c) => c.slug === categorySlug) : undefined;

  const [query, setQuery] = useState("");
  // Seeded from the current selection so returning to this step (e.g. Back from Review) opens
  // on the selected category's siblings rather than dumping the user back at the top level.
  const [navStack, setNavStack] = useState<CategoryResponse[]>(() =>
    selected ? categoryAncestors(categories, selected).slice(0, -1) : [],
  );

  if (loading) return <LoadingState label="Loading categories…" />;
  if (error) return <ErrorState message={error} />;

  const current = navStack[navStack.length - 1];
  const parent = navStack[navStack.length - 2];
  const list = (current ? childrenOf(categories, current.id) : rootsOf(categories))
    .slice()
    .sort((a, b) => a.displayOrder - b.displayOrder);

  const trimmedQuery = query.trim();
  const searchResults =
    trimmedQuery.length > 0
      ? categories
          .filter((c) => c.name.toLowerCase().includes(trimmedQuery.toLowerCase()))
          .sort((a, b) => a.name.localeCompare(b.name))
          .slice(0, 20)
      : [];

  const selectLeaf = (category: CategoryResponse) => {
    onSelect(category.slug, pathFor(categories, category));
    setQuery("");
  };

  const handleRowClick = (category: CategoryResponse) => {
    if (hasChildren(categories, category.id)) {
      setNavStack([...navStack, category]);
    } else {
      selectLeaf(category);
    }
  };

  const handleSearchResultClick = (category: CategoryResponse) => {
    if (hasChildren(categories, category.id)) {
      setNavStack(categoryAncestors(categories, category));
      setQuery("");
    } else {
      selectLeaf(category);
    }
  };

  return (
    <div className="post-ad-step">
      <h2 className="post-ad-step__title">What are you advertising?</h2>
      <p className="post-ad-step__subtitle">Choose the category that best fits your ad.</p>

      <div className="category-step">
        <div className="category-step__search">
          <FaSearch aria-hidden="true" />
          <label htmlFor="category-step-search" className="visually-hidden">
            Search category
          </label>
          <input
            id="category-step-search"
            type="text"
            placeholder="Search category…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>

        {searchResults.length > 0 && (
          <ul className="category-step__results">
            {searchResults.map((category) => (
              <li key={category.id}>
                <button
                  type="button"
                  className="category-step__result"
                  onClick={() => handleSearchResultClick(category)}
                >
                  <span className="category-step__result-name">{category.name}</span>
                  <span className="category-step__result-path">{pathFor(categories, category)}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        {trimmedQuery.length > 0 && searchResults.length === 0 && (
          <p className="category-step__no-results">No categories match "{trimmedQuery}"</p>
        )}

        {selected && (
          <p className="category-step__selected-path">
            Selected: <strong>{pathFor(categories, selected)}</strong>
          </p>
        )}

        {trimmedQuery.length === 0 && (
          <div className="category-step__browse">
            {current && (
              <button type="button" className="category-step__browse-back" onClick={() => setNavStack(navStack.slice(0, -1))}>
                <FaChevronRight aria-hidden="true" style={{ transform: "rotate(180deg)" }} />
                Back to {parent ? parent.name : "all categories"}
              </button>
            )}
            <ul className="category-step__browse-list">
              {list.map((category) => {
                const Icon = getCategoryIcon(category.slug);
                const isSelected = categorySlug === category.slug;
                return (
                  <li key={category.id}>
                    <button
                      type="button"
                      className={`category-step__browse-row ${isSelected ? "category-step__browse-row--selected" : ""}`}
                      onClick={() => handleRowClick(category)}
                    >
                      <span className="category-step__browse-row-name">
                        <Icon aria-hidden="true" className="category-step__browse-row-icon" />
                        {category.name}
                      </span>
                      {hasChildren(categories, category.id) ? (
                        <FaChevronRight aria-hidden="true" />
                      ) : (
                        isSelected && <FaCheckCircle aria-hidden="true" className="category-step__check" />
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
