import type { AttributeDefinitionResponse } from "../../types/api";

interface NumberAttributeFieldProps {
  definition: AttributeDefinitionResponse;
  value: string;
  onChange: (value: string) => void;
  error?: string;
}

export function NumberAttributeField({ definition, value, onChange, error }: NumberAttributeFieldProps) {
  const inputId = `attr-${definition.key}`;
  return (
    <div className="attribute-field">
      <label htmlFor={inputId}>
        {definition.name}
        {definition.required && <span className="attribute-field__required">*</span>}
        {definition.unit ? ` (${definition.unit})` : ""}
      </label>
      <input id={inputId} type="number" value={value} onChange={(e) => onChange(e.target.value)} />
      {error && <span className="attribute-field__error">{error}</span>}
    </div>
  );
}
