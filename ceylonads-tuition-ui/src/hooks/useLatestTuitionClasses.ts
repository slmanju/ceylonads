import { useEffect, useState } from "react";
import { isAxiosError } from "axios";
import { tuitionRepository } from "../tuition/api/tuitionApi";
import type { TuitionClassCardResponse } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";

export interface UseLatestTuitionClassesResult {
  classes: TuitionClassCardResponse[];
  totalPages: number;
  loading: boolean;
  error: string | null;
}

// Backs the homepage "Latest Classes" section: one paginated request to the isolated GET
// /api/tuition/classes endpoint, independent of the generic /api/ads search (see
// ceylonads-api's TuitionClassService.getLatest).
export function useLatestTuitionClasses(page: number, size: number): UseLatestTuitionClassesResult {
  const [classes, setClasses] = useState<TuitionClassCardResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();
    setLoading(true);
    setError(null);

    tuitionRepository
      .getLatestClasses(page, size, controller.signal)
      .then((data) => {
        if (cancelled) return;
        setClasses(data.content);
        setTotalPages(data.totalPages);
      })
      .catch((err) => {
        if (cancelled || (isAxiosError(err) && err.code === "ERR_CANCELED")) return;
        setError(getApiErrorMessage(err, "Could not load classes."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [page, size]);

  return { classes, totalPages, loading, error };
}
