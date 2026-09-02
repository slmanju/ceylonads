import type { ReactNode } from "react";
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
  /** An extra full-width item (Search Page Spotlight's mobile/tablet inline card - see
   *  ClassSearchResults) spliced into this same grid after the given number of organic cards, so
   *  it shares the grid's own column/gap layout. CSS-hidden by default (see ClassGrid.css); a
   *  narrow-viewport media query is what actually shows it, since the same promotion also renders
   *  in the desktop right rail and the two are mutually exclusive per breakpoint. */
  insertAfter?: { index: number; node: ReactNode };
}

// Organic tuition results only - sponsored content lives in the dedicated SearchBoostSection above
// this grid and the Search Page Spotlight rail/inline card (see ClassSearchResults), never mixed
// into these cards, so the result count and pagination the page shows always reflect real backend
// search results.
export function ClassGrid({
  ads,
  loading = false,
  error = null,
  onRetry,
  emptyTitle = "No classes found",
  emptyMessage = "Try adjusting your search or filters.",
  detailsById,
  insertAfter,
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

  const cards: ReactNode[] = ads.map((ad) => <ClassCard key={ad.id} ad={ad} details={detailsById?.get(ad.id)} />);
  if (insertAfter) {
    cards.splice(
      Math.min(insertAfter.index, cards.length),
      0,
      <div className="class-grid__insert" key="class-grid-insert">
        {insertAfter.node}
      </div>,
    );
  }

  return <div className="class-grid">{cards}</div>;
}
