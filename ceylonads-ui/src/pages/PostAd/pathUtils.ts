import type { CategoryResponse } from "../../types/api";

export function buildCategoryPath(categories: CategoryResponse[], slug: string): string {
  const category = categories.find((c) => c.slug === slug);
  if (!category) return "";
  if (category.parentId === null) return category.name;
  const parent = categories.find((c) => c.id === category.parentId);
  return parent ? `${parent.name} › ${category.name}` : category.name;
}
