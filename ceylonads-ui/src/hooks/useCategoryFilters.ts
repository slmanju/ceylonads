import { useEffect, useState } from "react";
import { getCategoryFilters } from "../api/categoryApi";
import type { AttributeDefinitionResponse } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";
import { cachedRequest } from "../utils/requestCache";

export function useCategoryFilters(categorySlug: string) {
  const [definitions, setDefinitions] = useState<AttributeDefinitionResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!categorySlug) {
      setDefinitions([]);
      setError(null);
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    cachedRequest(`category-filters:${categorySlug}`, () => getCategoryFilters(categorySlug))
      .then((data) => {
        if (!cancelled) setDefinitions(data.filters);
      })
      .catch((err) => {
        if (!cancelled) setError(getApiErrorMessage(err, "Could not load filters for this category."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [categorySlug]);

  return { definitions, loading, error };
}
