import type { AdResponse } from "../../types/api";
import { AdCard } from "../AdCard/AdCard";
import { LoadingState } from "../LoadingState/LoadingState";
import { EmptyState } from "../EmptyState/EmptyState";
import { ErrorState } from "../ErrorState/ErrorState";
import "./AdGrid.css";

interface AdGridProps {
  ads: AdResponse[];
  loading?: boolean;
  error?: string | null;
  onRetry?: () => void;
  emptyTitle?: string;
  emptyMessage?: string;
  // "tuition" renders the 3-column (desktop) / 2 (tablet) / 1 (mobile) grid the Tuition search
  // results grid uses instead of the generic 4/3/2 grid, so wider portrait poster cards fit.
  variant?: "default" | "tuition";
}

export function AdGrid({
  ads,
  loading = false,
  error = null,
  onRetry,
  emptyTitle = "No ads found",
  emptyMessage = "Try adjusting your search or filters.",
  variant = "default",
}: AdGridProps) {
  if (loading) {
    return <LoadingState label="Loading ads…" />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={onRetry} />;
  }

  if (ads.length === 0) {
    return <EmptyState title={emptyTitle} message={emptyMessage} />;
  }

  const gridClassName = `listing-grid ${variant === "tuition" ? "listing-grid--tuition" : ""}`.trim();

  return (
    <div className={gridClassName}>
      {ads.map((ad) => (
        <AdCard key={ad.id} ad={ad} />
      ))}
    </div>
  );
}
