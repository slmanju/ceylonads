import type { AdAttributeResponse, AdResponse } from "../types/api";
import type { Curriculum } from "../tuition/model/tuition";

// Ad attributes are backend-driven dynamic key/value pairs, not fixed schema fields — a
// subcategory without these keys defined (see the empty attribute lists for e.g. Higher
// Education) simply omits them, which callers must treat as "nothing to show" rather than an
// error. Keys mirror the seeded school-tuition attribute set (subject, grade, medium, classMode,
// classType) but are looked up defensively so nothing breaks if a category doesn't define them.
function findAttr(ad: AdResponse, key: string): AdAttributeResponse | undefined {
  return ad.attributes.find((a) => a.key === key);
}

export function getAttrDisplay(ad: AdResponse, key: string): string | null {
  const attr = findAttr(ad, key);
  return attr && attr.displayValue.trim() !== "" ? attr.displayValue : null;
}

export function getAttrValue(ad: AdResponse, key: string): string | null {
  const attr = findAttr(ad, key);
  return attr && attr.value.trim() !== "" ? attr.value : null;
}

export function isOnlineClass(ad: AdResponse): boolean {
  const mode = getAttrValue(ad, "classMode");
  return mode === "ONLINE" || mode === "BOTH";
}

// Maps the real "curriculum" attribute's stable stored value (school-tuition only) onto the
// promotion/mock layer's Curriculum enum, so promotion eligibility (see tuition/promotion) can be
// matched against real attribute/backend data without a second, drifting copy of this table.
const CURRICULUM_ATTRIBUTE_MAP: Record<string, Curriculum> = {
  LOCAL: "SRI_LANKAN_NATIONAL",
  CAMBRIDGE: "CAMBRIDGE",
  EDEXCEL: "PEARSON_EDEXCEL",
  IB: "OTHER",
  PROFESSIONAL: "OTHER",
};

export function curriculumEnumFromValue(rawValue: string | null | undefined): Curriculum | undefined {
  return rawValue ? CURRICULUM_ATTRIBUTE_MAP[rawValue] : undefined;
}
