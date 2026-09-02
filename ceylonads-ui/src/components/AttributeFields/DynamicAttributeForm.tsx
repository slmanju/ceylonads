import type { AttributeDefinitionResponse } from "../../types/api";
import { AttributeField } from "./AttributeField";
import "./AttributeFields.css";

interface DynamicAttributeFormProps {
  definitions: AttributeDefinitionResponse[];
  mode: "form" | "filter";
  values: Record<string, string>;
  errors?: Record<string, string>;
  onChange: (key: string, value: string) => void;
}

export function DynamicAttributeForm({ definitions, mode, values, errors, onChange }: DynamicAttributeFormProps) {
  const sorted = [...definitions].sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <div className="dynamic-attribute-form">
      {sorted.map((definition) => (
        <AttributeField
          key={definition.id}
          definition={definition}
          mode={mode}
          values={values}
          onChange={onChange}
          error={errors?.[definition.key]}
        />
      ))}
    </div>
  );
}
