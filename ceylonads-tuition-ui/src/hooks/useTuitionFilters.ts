import { useEffect, useState } from "react";
import { tuitionRepository } from "../tuition/api/tuitionApi";
import type { TuitionFilterMetadataResponse } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";
import { cachedRequest } from "../utils/requestCache";

// Master data for the top filter bar (subjects/levels/curricula/mediums/deliveryModes) spans the
// whole tuition vertical and never varies by category/location/query, so it's fetched once and
// cached across every ClassSearchResults mount (Classes, Tutors, Online Classes, ...) rather than
// re-requested per page.
export function useTuitionFilters() {
  const [data, setData] = useState<TuitionFilterMetadataResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    cachedRequest("tuition-filters", () => tuitionRepository.getFilters())
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err) => {
        if (!cancelled) setError(getApiErrorMessage(err, "Could not load tuition filters."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return { data, loading, error };
}
