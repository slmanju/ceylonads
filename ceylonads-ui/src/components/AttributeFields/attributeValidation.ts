import type { AttributeDefinitionResponse } from "../../types/api";

// Metadata-driven early feedback only - required + basic type checks. The backend stays the
// authority (option membership, exact numeric rules, unknown-key rejection all happen there).
export function validateAttributes(
  definitions: AttributeDefinitionResponse[],
  values: Record<string, string>,
): Record<string, string> {
  const errors: Record<string, string> = {};

  for (const definition of definitions) {
    const raw = (values[definition.key] ?? "").trim();

    if (definition.required && !raw) {
      errors[definition.key] = `${definition.name} is required.`;
      continue;
    }
    if (!raw) continue;

    if (definition.dataType === "NUMBER" || definition.dataType === "DECIMAL") {
      const numeric = Number(raw);
      if (Number.isNaN(numeric)) {
        errors[definition.key] = `${definition.name} must be a number.`;
      } else if (definition.dataType === "NUMBER" && !Number.isInteger(numeric)) {
        errors[definition.key] = `${definition.name} must be a whole number.`;
      }
    }
  }

  return errors;
}
