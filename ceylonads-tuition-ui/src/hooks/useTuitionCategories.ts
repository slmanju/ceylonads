import { useMemo } from "react";
import { useCategories } from "./useCategories";
import { childrenOf } from "../utils/hierarchy";
import type { CategoryResponse } from "../types/api";

// Every tuition-relevant category on the shared backend lives under this one stable root slug
// (see ceylonads-ui's PostAd LocationStep, which keys the same behaviour off this slug). Never a
// hardcoded id — if the root category is ever missing, the tuition site degrades to an empty
// category list rather than guessing an id.
export const TUITION_ROOT_SLUG = "education-tuition";

export interface TuitionCategories {
  root: CategoryResponse | undefined;
  /** Direct subject-area subcategories (School Tuition, Language Classes, etc.). */
  subcategories: CategoryResponse[];
  /** root + subcategories, keyed by slug, for quick lookups. */
  bySlug: Map<string, CategoryResponse>;
  loading: boolean;
  error: string | null;
}

export function useTuitionCategories(): TuitionCategories {
  const { categories, loading, error } = useCategories();

  return useMemo(() => {
    const root = categories.find((c) => c.slug === TUITION_ROOT_SLUG);
    const subcategories = root
      ? childrenOf(categories, root.id).slice().sort((a, b) => a.displayOrder - b.displayOrder)
      : [];
    const bySlug = new Map<string, CategoryResponse>();
    if (root) bySlug.set(root.slug, root);
    for (const c of subcategories) bySlug.set(c.slug, c);

    return { root, subcategories, bySlug, loading, error };
  }, [categories, loading, error]);
}
