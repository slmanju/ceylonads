import type { ClassFormat, ClassPurpose } from "./tuition";

// Single source of truth for the tuition search/filter state, read from and written back to the
// URL by ClassSearchResults. Shared by the desktop top filter bar, the mobile filter drawer, and
// the active-filter chips so none of them can drift out of sync with each other.
//
// subject/level/curriculum/medium/deliveryMode hold the stable option `value` from
// GET /api/tuition/filters (e.g. "PHYSICS", "AL") and are sent to the backend as real
// attr.<key> filters - never client-side-only matching. classFormats/classPurposes remain a
// decorative, mock-provider-only layer (see tuition/model/tuition.ts) applied client-side to the
// already-fetched results page, since the backend has no such attributes yet.
export interface ClassFilterValues {
  q: string;
  category: string;
  location: string;
  subject: string;
  level: string;
  curriculum: string;
  medium: string;
  deliveryMode: string;
  minPrice: string;
  maxPrice: string;
  classFormats: ClassFormat[];
  classPurposes: ClassPurpose[];
}

export function emptyClassFilterValues(overrides: Partial<ClassFilterValues> = {}): ClassFilterValues {
  return {
    q: "",
    category: "",
    location: "",
    subject: "",
    level: "",
    curriculum: "",
    medium: "",
    deliveryMode: "",
    minPrice: "",
    maxPrice: "",
    classFormats: [],
    classPurposes: [],
    ...overrides,
  };
}
