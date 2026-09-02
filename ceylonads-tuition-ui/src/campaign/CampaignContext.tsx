import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { getActiveTuitionCampaign } from "../api/promotionApi";
import type { TuitionCampaignResponse } from "../types/api";

interface CampaignContextValue {
  campaign: TuitionCampaignResponse | null;
  loading: boolean;
}

const CampaignContext = createContext<CampaignContextValue | undefined>(undefined);

// One fetch of GET /api/tuition/promotions/campaign at app-layout level, shared by CampaignBanner
// and CampaignModal so neither issues its own request. Campaign marketing is non-critical UI - a
// failed fetch just leaves campaign null (no banner/modal), it never blocks the rest of the app.
export function CampaignProvider({ children }: { children: ReactNode }) {
  const [campaign, setCampaign] = useState<TuitionCampaignResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    getActiveTuitionCampaign()
      .then((result) => {
        if (!cancelled) setCampaign(result);
      })
      .catch(() => {
        // Fail quietly - see module comment above.
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return <CampaignContext.Provider value={{ campaign, loading }}>{children}</CampaignContext.Provider>;
}

export function useCampaign(): CampaignContextValue {
  const ctx = useContext(CampaignContext);
  if (!ctx) {
    throw new Error("useCampaign must be used within a CampaignProvider");
  }
  return ctx;
}
