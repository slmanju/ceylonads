import { useEffect, useState } from "react";
import { tuitionRepository } from "../tuition/api/tuitionApi";
import type { TuitionDetails } from "../tuition/model/tuition";
import type { AdResponse, LocationResponse } from "../types/api";

// One ad (e.g. the class detail page). `locations` should be the full location list from
// useLocations() so the provider can resolve realistic secondary/home-visit areas.
export function useTuitionDetails(ad: AdResponse | null, locations: LocationResponse[]) {
  const [details, setDetails] = useState<TuitionDetails | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!ad) {
      setDetails(null);
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);

    tuitionRepository
      .getDetails(ad, locations)
      .then((data) => {
        if (!cancelled) setDetails(data);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [ad, locations]);

  return { details, loading };
}

// A page of ads (e.g. search results). Keyed by ad id for O(1) lookup from ClassGrid/ClassCard.
export function useTuitionDetailsMap(ads: AdResponse[], locations: LocationResponse[]) {
  const [detailsById, setDetailsById] = useState<Map<number, TuitionDetails>>(new Map());
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (ads.length === 0) {
      setDetailsById(new Map());
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);

    tuitionRepository
      .getDetailsMap(ads, locations)
      .then((map) => {
        if (!cancelled) setDetailsById(map);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [ads, locations]);

  return { detailsById, loading };
}
