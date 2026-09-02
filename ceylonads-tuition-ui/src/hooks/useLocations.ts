import { useEffect, useState } from "react";
import { listLocations } from "../api/locationApi";
import type { LocationResponse } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";
import { cachedRequest } from "../utils/requestCache";

export function useLocations() {
  const [locations, setLocations] = useState<LocationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    cachedRequest("locations", listLocations)
      .then((data) => {
        if (!cancelled) setLocations(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getApiErrorMessage(err, "Could not load locations."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return { locations, loading, error };
}
