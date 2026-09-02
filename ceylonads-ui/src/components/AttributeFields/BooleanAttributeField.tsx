import type { AttributeDefinitionResponse } from "../../types/api";

interface BooleanAttributeFieldProps {
  definition: AttributeDefinitionResponse;
  mode: "form" | "filter";
  value: string;
  onChange: (value: string) => void;
}

export function BooleanAttributeField({ definition, mode, value, onChange }: BooleanAttributeFieldProps) {
  const inputId = `attr-${definition.key}`;

  if (mode === "filter") {
    return (
      <div className="attribute-field">
        <label htmlFor={inputId}>{definition.name}</label>
        <select id={inputId} value={value} onChange={(e) => onChange(e.target.value)}>
          <option value="">Any</option>
          <option value="true">Yes</option>
          <option value="false">No</option>
        </select>
      </div>
    );
  }

  return (
    <div className="attribute-field attribute-field--checkbox">
      <label htmlFor={inputId}>
        <input
          id={inputId}
          type="checkbox"
          checked={value === "true"}
          onChange={(e) => onChange(e.target.checked ? "true" : "false")}
        />
        {definition.name}
      </label>
    </div>
  );
}
