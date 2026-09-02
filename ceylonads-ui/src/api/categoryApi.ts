import { apiClient } from "./apiClient";
import type {
  AdminCategoryRequest,
  AttributeDefinitionResponse,
  CategoryFiltersResponse,
  CategoryResponse,
} from "../types/api";

export async function listCategories(): Promise<CategoryResponse[]> {
  const { data } = await apiClient.get<CategoryResponse[]>("/api/categories");
  return data;
}

export async function createCategory(payload: AdminCategoryRequest): Promise<CategoryResponse> {
  const { data } = await apiClient.post<CategoryResponse>("/api/admin/categories", payload);
  return data;
}

export async function getCategoryAttributes(slug: string): Promise<AttributeDefinitionResponse[]> {
  const { data } = await apiClient.get<AttributeDefinitionResponse[]>(`/api/categories/${slug}/attributes`);
  return data;
}

export async function getCategoryFilters(slug: string): Promise<CategoryFiltersResponse> {
  const { data } = await apiClient.get<CategoryFiltersResponse>(`/api/categories/${slug}/filters`);
  return data;
}
