import "./SubjectFilters.css";

interface FeeRangeFieldsProps {
  minPrice: string;
  maxPrice: string;
  onChange: (minPrice: string, maxPrice: string) => void;
  idPrefix?: string;
}

// Shared by the desktop Fee popover and the mobile filter drawer so both read/write the same
// values without duplicating the min/max validation logic.
export function FeeRangeFields({ minPrice, maxPrice, onChange, idPrefix = "filter" }: FeeRangeFieldsProps) {
  const min = minPrice ? Number(minPrice) : null;
  const max = maxPrice ? Number(maxPrice) : null;
  const invalid = min !== null && max !== null && min > max;

  return (
    <div className="subject-filters__group">
      <label htmlFor={`${idPrefix}-min-price`}>Fee (Rs. / month)</label>
      <div className="subject-filters__price-row">
        <input
          id={`${idPrefix}-min-price`}
          type="number"
          min={0}
          placeholder="Min"
          value={minPrice}
          onChange={(e) => onChange(e.target.value, maxPrice)}
        />
        <span aria-hidden="true">–</span>
        <label htmlFor={`${idPrefix}-max-price`} className="visually-hidden">
          Maximum fee
        </label>
        <input
          id={`${idPrefix}-max-price`}
          type="number"
          min={0}
          placeholder="Max"
          value={maxPrice}
          onChange={(e) => onChange(minPrice, e.target.value)}
        />
      </div>
      {invalid && <span className="subject-filters__error">Min fee is higher than max fee.</span>}
    </div>
  );
}
