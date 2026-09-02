import { FaChevronLeft, FaChevronRight } from "react-icons/fa";
import "./Pagination.css";

interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

function pageWindow(current: number, total: number): number[] {
  const span = 2;
  const start = Math.max(0, Math.min(current - span, total - 5));
  const end = Math.min(total, start + 5);
  return Array.from({ length: end - start }, (_, i) => start + i);
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        type="button"
        className="pagination__nav"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
      >
        <FaChevronLeft aria-hidden="true" /> Previous
      </button>

      <div className="pagination__pages">
        {pageWindow(page, totalPages).map((p) => (
          <button
            key={p}
            type="button"
            className={`pagination__page ${p === page ? "pagination__page--active" : ""}`}
            aria-current={p === page ? "page" : undefined}
            onClick={() => onPageChange(p)}
          >
            {p + 1}
          </button>
        ))}
      </div>

      <button
        type="button"
        className="pagination__nav"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        Next <FaChevronRight aria-hidden="true" />
      </button>
    </nav>
  );
}
