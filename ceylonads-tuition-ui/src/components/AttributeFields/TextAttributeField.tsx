import type { AttributeDefinitionResponse } from "../../types/api";

interface TextAttributeFieldProps {
  definition: AttributeDefinitionResponse;
  value: string;
  onChange: (value: string) => void;
  error?: string;
}

export function TextAttributeField({ definition, value, onChange, error }: TextAttributeFieldProps) {
  const inputId = `attr-${definition.key}`;
  return (
    <div className="attribute-field">
      <label htmlFor={inputId}>
        {definition.name}
        {definition.required && <span className="attribute-field__required">*</span>}
      </label>
      <input id={inputId} type="text" value={value} onChange={(e) => onChange(e.target.value)} />
      {error && <span className="attribute-field__error">{error}</span>}
    </div>
  );
}
