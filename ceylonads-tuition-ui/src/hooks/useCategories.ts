import { useEffect, useState } from "react";
import { listCategories } from "../api/categoryApi";
import type { CategoryResponse } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";
import { cachedRequest } from "../utils/requestCache";

// Full CeylonAds category tree — used only to resolve the tuition subtree below. Pages that
// display categories to the user should read from useTuitionCategories instead, so the tuition
// site never surfaces the generic marketplace taxonomy.
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
