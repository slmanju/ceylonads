import { useEffect, useState } from "react";
import { getCategoryAttributes } from "../api/categoryApi";
import type { AttributeDefinitionResponse } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";

export function useCategoryAttributes(categorySlug: string) {
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

    getCategoryAttributes(categorySlug)
      .then((data) => {
        if (!cancelled) setDefinitions(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getApiErrorMessage(err, "Could not load category attributes."));
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
