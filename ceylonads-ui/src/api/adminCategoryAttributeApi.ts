import { apiClient } from "./apiClient";
import type {
  AdminAttributeDefinitionRequest,
  AdminAttributeDefinitionUpdateRequest,
  AdminAttributeOptionRequest,
  AdminAttributeOptionUpdateRequest,
  AttributeDefinitionResponse,
  AttributeOptionResponse,
} from "../types/api";

export async function listAttributeDefinitions(categoryId: number): Promise<AttributeDefinitionResponse[]> {
  const { data } = await apiClient.get<AttributeDefinitionResponse[]>(`/api/admin/categories/${categoryId}/attributes`);
  return data;
}

export async function createAttributeDefinition(
  categoryId: number,
  payload: AdminAttributeDefinitionRequest,
): Promise<AttributeDefinitionResponse> {
  const { data } = await apiClient.post<AttributeDefinitionResponse>(
    `/api/admin/categories/${categoryId}/attributes`,
    payload,
  );
  return data;
}

export async function updateAttributeDefinition(
  categoryId: number,
  attributeId: number,
  payload: AdminAttributeDefinitionUpdateRequest,
): Promise<AttributeDefinitionResponse> {
  const { data } = await apiClient.put<AttributeDefinitionResponse>(
    `/api/admin/categories/${categoryId}/attributes/${attributeId}`,
    payload,
  );
  return data;
}

export async function setAttributeDefinitionActive(
  categoryId: number,
  attributeId: number,
  active: boolean,
): Promise<AttributeDefinitionResponse> {
  const { data } = await apiClient.patch<AttributeDefinitionResponse>(
    `/api/admin/categories/${categoryId}/attributes/${attributeId}/active`,
    null,
    { params: { active } },
  );
  return data;
}

export async function createAttributeOption(
  categoryId: number,
  attributeId: number,
  payload: AdminAttributeOptionRequest,
): Promise<AttributeOptionResponse> {
  const { data } = await apiClient.post<AttributeOptionResponse>(
    `/api/admin/categories/${categoryId}/attributes/${attributeId}/options`,
    payload,
  );
  return data;
}

export async function updateAttributeOption(
  categoryId: number,
  attributeId: number,
  optionId: number,
  payload: AdminAttributeOptionUpdateRequest,
): Promise<AttributeOptionResponse> {
  const { data } = await apiClient.put<AttributeOptionResponse>(
    `/api/admin/categories/${categoryId}/attributes/${attributeId}/options/${optionId}`,
    payload,
  );
  return data;
}

export async function setAttributeOptionActive(
  categoryId: number,
  attributeId: number,
  optionId: number,
  active: boolean,
): Promise<AttributeOptionResponse> {
  const { data } = await apiClient.patch<AttributeOptionResponse>(
    `/api/admin/categories/${categoryId}/attributes/${attributeId}/options/${optionId}/active`,
    null,
    { params: { active } },
  );
  return data;
}
