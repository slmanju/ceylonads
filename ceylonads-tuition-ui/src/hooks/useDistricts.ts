import { useMemo } from "react";
import { useLocations } from "./useLocations";
import type { LocationResponse } from "../types/api";

export function useDistricts(): { districts: LocationResponse[]; loading: boolean; error: string | null } {
  const { locations, loading, error } = useLocations();

  const districts = useMemo(
    () => locations.filter((l) => l.type === "DISTRICT").sort((a, b) => a.name.localeCompare(b.name)),
    [locations],
  );

  return { districts, loading, error };
}
