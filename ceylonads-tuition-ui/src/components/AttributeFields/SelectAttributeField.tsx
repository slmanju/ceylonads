import type { AttributeDefinitionResponse } from "../../types/api";

interface SelectAttributeFieldProps {
  definition: AttributeDefinitionResponse;
  multi: boolean;
  mode: "form" | "filter";
  value: string;
  onChange: (value: string) => void;
  error?: string;
}

export function SelectAttributeField({ definition, multi, mode, value, onChange, error }: SelectAttributeFieldProps) {
  const inputId = `attr-${definition.key}`;
  const options = definition.options;

  // A multi-select filter (any ad containing one chosen value) is still expressible as a plain
  // dropdown, so only the Post Ad form needs the checkbox-group UI for MULTI_SELECT.
  if (multi && mode === "form") {
    const selected = new Set(value ? value.split(",") : []);
    const toggle = (optionValue: string) => {
      const next = new Set(selected);
      if (next.has(optionValue)) next.delete(optionValue);
      else next.add(optionValue);
      onChange(Array.from(next).join(","));
    };

    return (
      <div className="attribute-field">
        <span className="attribute-field__label">
          {definition.name}
          {definition.required && <span className="attribute-field__required">*</span>}
        </span>
        <div className="attribute-field__checkbox-group">
          {options.map((option) => (
            <label key={option.value} className="attribute-field__checkbox-option">
              <input type="checkbox" checked={selected.has(option.value)} onChange={() => toggle(option.value)} />
              {option.label}
            </label>
          ))}
        </div>
        {error && <span className="attribute-field__error">{error}</span>}
      </div>
    );
  }

  return (
    <div className="attribute-field">
      <label htmlFor={inputId}>
        {definition.name}
        {mode === "form" && definition.required && <span className="attribute-field__required">*</span>}
      </label>
      <select id={inputId} value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">{mode === "filter" ? "Any" : `Select ${definition.name.toLowerCase()}`}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && <span className="attribute-field__error">{error}</span>}
    </div>
  );
}
