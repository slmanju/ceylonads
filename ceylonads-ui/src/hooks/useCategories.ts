import { useEffect, useState } from "react";
import { listCategories } from "../api/categoryApi";
import type { CategoryResponse } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";
import { cachedRequest } from "../utils/requestCache";

export function useCategories() {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    cachedRequest("categories", listCategories)
      .then((data) => {
        if (!cancelled) setCategories(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getApiErrorMessage(err, "Could not load categories."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return { categories, loading, error };
}
