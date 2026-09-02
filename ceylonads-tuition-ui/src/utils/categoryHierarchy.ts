import type { CategoryResponse } from "../types/api";

/** Root-to-leaf ancestor chain for a category, including the category itself. */
export function categoryAncestors(categories: CategoryResponse[], category: CategoryResponse): CategoryResponse[] {
  const chain: CategoryResponse[] = [];
  let current: CategoryResponse | undefined = category;
  while (current) {
    chain.unshift(current);
    const parentId: number | null = current.parentId;
    current = parentId ? categories.find((c) => c.id === parentId) : undefined;
  }
  return chain;
}
