import type { AttributeDefinitionResponse } from "../../types/api";
import { TextAttributeField } from "./TextAttributeField";
import { NumberAttributeField } from "./NumberAttributeField";
import { SelectAttributeField } from "./SelectAttributeField";
import "./AttributeFields.css";

interface AttributeFieldProps {
  definition: AttributeDefinitionResponse;
  mode: "form" | "filter";
  values: Record<string, string>;
  onChange: (key: string, value: string) => void;
  error?: string;
}

export function AttributeField({ definition, mode, values, onChange, error }: AttributeFieldProps) {
  const value = values[definition.key] ?? "";
  const setValue = (v: string) => onChange(definition.key, v);

  switch (definition.dataType) {
    case "TEXT":
      return <TextAttributeField definition={definition} value={value} onChange={setValue} error={error} />;
    case "NUMBER":
    case "DECIMAL":
      return <NumberAttributeField definition={definition} value={value} onChange={setValue} error={error} />;
    case "SELECT":
      return (
        <SelectAttributeField definition={definition} multi={false} mode={mode} value={value} onChange={setValue} error={error} />
      );
    case "MULTI_SELECT":
      return (
        <SelectAttributeField definition={definition} multi={true} mode={mode} value={value} onChange={setValue} error={error} />
      );
    default:
      return null;
  }
}
