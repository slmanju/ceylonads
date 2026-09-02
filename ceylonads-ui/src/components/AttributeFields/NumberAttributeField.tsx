import type { AttributeDefinitionResponse } from "../../types/api";

interface NumberAttributeFieldProps {
  definition: AttributeDefinitionResponse;
  mode: "form" | "filter";
  values: Record<string, string>;
  onChange: (key: string, value: string) => void;
  error?: string;
}

export function NumberAttributeField({ definition, mode, values, onChange, error }: NumberAttributeFieldProps) {
  const step = definition.dataType === "DECIMAL" ? "any" : 1;
  const inputId = `attr-${definition.key}`;

  if (mode === "filter") {
    const minKey = `${definition.key}.min`;
    const maxKey = `${definition.key}.max`;
    return (
      <div className="attribute-field">
        <span className="attribute-field__label">
          {definition.name}
          {definition.unit ? ` (${definition.unit})` : ""}
        </span>
        <div className="attribute-field__range-row">
          <input
            type="number"
            step={step}
            placeholder="Min"
            aria-label={`${definition.name} minimum`}
            value={values[minKey] ?? ""}
            onChange={(e) => onChange(minKey, e.target.value)}
          />
          <span aria-hidden="true">–</span>
          <input
            type="number"
            step={step}
            placeholder="Max"
            aria-label={`${definition.name} maximum`}
            value={values[maxKey] ?? ""}
            onChange={(e) => onChange(maxKey, e.target.value)}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="attribute-field">
      <label htmlFor={inputId}>
        {definition.name}
        {definition.required && <span className="attribute-field__required">*</span>}
      </label>
      <div className="attribute-field__unit-row">
        <input
          id={inputId}
          type="number"
          step={step}
          value={values[definition.key] ?? ""}
          onChange={(e) => onChange(definition.key, e.target.value)}
        />
        {definition.unit && <span className="attribute-field__unit">{definition.unit}</span>}
      </div>
      {error && <span className="attribute-field__error">{error}</span>}
    </div>
  );
}
