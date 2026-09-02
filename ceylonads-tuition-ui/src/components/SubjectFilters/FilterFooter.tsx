import "./FilterFooter.css";

interface FilterFooterProps {
  onReset: () => void;
  onApply: () => void;
}

export function FilterFooter({ onReset, onApply }: FilterFooterProps) {
  return (
    <div className="filter-footer">
      <button type="button" className="btn btn-secondary" onClick={onReset} style={{ flex: 1 }}>
        Reset
      </button>
      <button type="button" className="btn btn-accent" onClick={onApply} style={{ flex: 1 }}>
        Search
      </button>
    </div>
  );
}
