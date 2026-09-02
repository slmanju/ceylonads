// Turns a display name like "Fuel Type" into a stable camelCase attribute key: "fuelType".
export function toAttributeKey(name: string): string {
  const words = name
    .trim()
    .split(/[^a-zA-Z0-9]+/)
    .filter(Boolean);
  if (words.length === 0) return "";

  return words
    .map((word, index) => {
      const lower = word.toLowerCase();
      return index === 0 ? lower : lower.charAt(0).toUpperCase() + lower.slice(1);
    })
    .join("");
}
