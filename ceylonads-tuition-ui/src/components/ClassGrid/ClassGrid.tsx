import type { AdResponse } from "../../types/api";
import type { TuitionDetails } from "../../tuition/model/tuition";
import { ClassCard } from "../ClassCard/ClassCard";
import { LoadingState } from "../LoadingState/LoadingState";
import { EmptyState } from "../EmptyState/EmptyState";
import { ErrorState } from "../ErrorState/ErrorState";
import "./ClassGrid.css";

interface ClassGridProps {
  ads: AdResponse[];
  loading?: boolean;
  error?: string | null;
  onRetry?: () => void;
  emptyTitle?: string;
  emptyMessage?: string;
  /** Tuition-specific metadata from the mock provider, keyed by ad id. */
  detailsById?: Map<number, TuitionDetails>;
}

// Organic tuition results only - sponsored content lives in the dedicated PromotionSidebar/top
// banner next to this grid (see ClassSearchResults), never mixed into these cards, so the result
// count and pagination the page shows always reflect real backend search results.
export function ClassGrid({
  ads,
  loading = false,
  error = null,
  onRetry,
  emptyTitle = "No classes found",
  emptyMessage = "Try adjusting your search or filters.",
  detailsById,
}: ClassGridProps) {
  if (loading) {
    return <LoadingState label="Loading classes…" />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={onRetry} />;
  }

  if (ads.length === 0) {
    return <EmptyState title={emptyTitle} message={emptyMessage} />;
  }

  return (
    <div className="class-grid">
      {ads.map((ad) => (
        <ClassCard key={ad.id} ad={ad} details={detailsById?.get(ad.id)} />
      ))}
    </div>
  );
}
