// Frontend-only tuition domain model. These types describe metadata the shared CeylonAds
// backend does not support yet (curriculum for non-school-tuition categories, schedule, home
// visit, teacher presentation, extended delivery/class taxonomy). Subject, grade, medium and
// curriculum already exist as real backend dynamic attributes on `school-tuition` (see
// utils/tuitionAttributes.ts) - every field below is read as a MOCK FALLBACK, used only when the
// real ad has no equivalent attribute (e.g. any ad outside `school-tuition`, which defines no
// dynamic attributes at all). Real values always win; nothing here overrides them.

export type DeliveryMode = "PHYSICAL" | "ONLINE" | "HYBRID" | "HOME_VISIT";

// How a class is structured (group size) - independent of ClassPurpose and DeliveryMode. A
// listing may support more than one, e.g. INDIVIDUAL + SMALL_GROUP.
export type ClassFormat = "INDIVIDUAL" | "SMALL_GROUP" | "GROUP" | "MASS_CLASS";

// Why/how the session runs, academic-flavoured and optional - non-academic activities (dancing,
// music, chess...) commonly have no class purpose at all.
export type ClassPurpose = "THEORY" | "REVISION" | "PAPER_CLASS" | "PRACTICAL";

// Curriculum is meaningless for non-academic learning (dancing, music, chess...), so it must stay
// optional rather than forcing a "NONE" value on the user.
export type Curriculum = "SRI_LANKAN_NATIONAL" | "CAMBRIDGE" | "PEARSON_EDEXCEL" | "OTHER";

export type Medium = "SINHALA" | "ENGLISH" | "TAMIL";

// Loose homepage-discovery grouping (e.g. "Music & Dancing", "Skills & Hobbies") - not a formal
// filter dimension like the others above, just a convenience tag for clustering non-academic
// templates into useful sections.
export type DiscoveryTheme = "ARTS" | "LANGUAGE" | "SKILL";

// Coarser than the real "grade" attribute (free text, e.g. "Grade 10", "2027 A/L") - used for
// grouping/discovery (homepage sections, level filter) where an exact grade string is too
// granular. Optional: non-academic activities (dancing, music, chess...) have no level at all.
export type TuitionLevel =
  | "PRIMARY"
  | "GRADE_6_9"
  | "OL"
  | "AL"
  | "IGCSE"
  | "AS_LEVEL"
  | "A_LEVEL"
  | "UNIVERSITY"
  | "PROFESSIONAL";

export type TeacherProfileType = "TEACHER" | "INSTITUTE";

export type DayOfWeek = "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY";

export interface TuitionSession {
  id: string;
  deliveryMode: DeliveryMode;
  locationId?: number;
  locationName?: string;
  venueName?: string;
  dayOfWeek: DayOfWeek;
  /** 24h "HH:mm" */
  startTime: string;
  /** 24h "HH:mm" */
  endTime: string;
  classFormats: ClassFormat[];
  /** Often empty for non-academic activities (dancing, music, chess...). */
  classPurposes: ClassPurpose[];
}

export interface HomeVisitServiceArea {
  locationId?: number;
  locationName: string;
}

export interface HomeVisitInfo {
  available: boolean;
  serviceAreas: HomeVisitServiceArea[];
  classFormats: ClassFormat[];
}

export interface TeacherProfile {
  profileType: TeacherProfileType;
  shortBio?: string;
  qualifications?: string[];
  experienceYears?: number;
  teachingSince?: number;
  affiliation?: string;
}

export interface TuitionDetails {
  adId: number;
  /** Mock fallback learning-area label (e.g. "Kandyan Dancing"), used only when the ad has no real "subject" attribute. */
  subjectLabel?: string;
  theme?: DiscoveryTheme;
  /** Mock fallback, used only when the ad has no real "grade" attribute. */
  level?: TuitionLevel;
  /** Mock fallback, used only when the ad has no real "curriculum" attribute. */
  curriculum?: Curriculum;
  /** Mock fallback, used only when the ad has no real "medium" attribute. Often empty (e.g. instrumental lessons). */
  mediums?: Medium[];
  targetExamYear?: string;
  /** Always includes "HOME_VISIT" when homeVisit.available is true - single source of truth. */
  deliveryModes: DeliveryMode[];
  classFormats: ClassFormat[];
  /** Often empty for non-academic activities - see ClassPurpose. */
  classPurposes: ClassPurpose[];
  sessions: TuitionSession[];
  homeVisit?: HomeVisitInfo;
  teacher?: TeacherProfile;
}

// Mock-layer-only criteria, applied client-side to the tuition metadata of an already-fetched
// backend results page. Each dimension is independent: selecting Home Visit does not require a
// class format, and selecting Group Class does not require a delivery mode. Curriculum/level are
// opt-in filters - leaving them unset keeps non-level subjects (dancing, music, chess...) fully
// discoverable.
export interface TuitionSearchCriteria {
  levels?: TuitionLevel[];
  curriculums?: Curriculum[];
  mediums?: Medium[];
  deliveryModes?: DeliveryMode[];
  classFormats?: ClassFormat[];
  classPurposes?: ClassPurpose[];
  themes?: DiscoveryTheme[];
  targetExamYear?: string;
}

export function isTuitionCriteriaEmpty(criteria: TuitionSearchCriteria): boolean {
  return (
    !criteria.levels?.length &&
    !criteria.curriculums?.length &&
    !criteria.mediums?.length &&
    !criteria.deliveryModes?.length &&
    !criteria.classFormats?.length &&
    !criteria.classPurposes?.length &&
    !criteria.themes?.length &&
    !criteria.targetExamYear
  );
}

export function matchesTuitionCriteria(details: TuitionDetails, criteria: TuitionSearchCriteria): boolean {
  if (criteria.levels?.length && (!details.level || !criteria.levels.includes(details.level))) {
    return false;
  }
  if (criteria.curriculums?.length && (!details.curriculum || !criteria.curriculums.includes(details.curriculum))) {
    return false;
  }
  if (criteria.mediums?.length && !criteria.mediums.some((m) => details.mediums?.includes(m))) {
    return false;
  }
  if (criteria.deliveryModes?.length && !criteria.deliveryModes.some((m) => details.deliveryModes.includes(m))) {
    return false;
  }
  if (criteria.classFormats?.length && !criteria.classFormats.some((f) => details.classFormats.includes(f))) {
    return false;
  }
  if (criteria.classPurposes?.length && !criteria.classPurposes.some((p) => details.classPurposes.includes(p))) {
    return false;
  }
  if (criteria.themes?.length && (!details.theme || !criteria.themes.includes(details.theme))) {
    return false;
  }
  if (criteria.targetExamYear) {
    const needle = criteria.targetExamYear.trim().toLowerCase();
    if (!details.targetExamYear || !details.targetExamYear.toLowerCase().includes(needle)) {
      return false;
    }
  }
  return true;
}
