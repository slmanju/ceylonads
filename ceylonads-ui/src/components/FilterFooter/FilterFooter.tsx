import "./FilterFooter.css";

interface FilterFooterProps {
  onReset: () => void;
  onApply: () => void;
}

export function FilterFooter({ onReset, onApply }: FilterFooterProps) {
  return (
    <div className="filter-footer">
      <button type="button" className="btn btn-secondary filter-footer__reset" onClick={onReset}>
        Reset all
      </button>
      <button type="button" className="btn btn-primary filter-footer__apply" onClick={onApply}>
        Search
      </button>
    </div>
  );
}
