import type {
  ClassFormat,
  ClassPurpose,
  Curriculum,
  DayOfWeek,
  DeliveryMode,
  Medium,
  TeacherProfileType,
  TuitionLevel,
} from "./tuition";

export const DELIVERY_MODE_LABELS: Record<DeliveryMode, string> = {
  PHYSICAL: "Physical",
  ONLINE: "Online",
  HYBRID: "Hybrid",
  HOME_VISIT: "Home Visit",
};

export const DELIVERY_MODE_ORDER: DeliveryMode[] = ["PHYSICAL", "ONLINE", "HYBRID", "HOME_VISIT"];

export const CLASS_FORMAT_LABELS: Record<ClassFormat, string> = {
  INDIVIDUAL: "Individual",
  SMALL_GROUP: "Small Group",
  GROUP: "Group Class",
  MASS_CLASS: "Mass Class",
};

export const CLASS_FORMAT_ORDER: ClassFormat[] = ["INDIVIDUAL", "SMALL_GROUP", "GROUP", "MASS_CLASS"];

export const CLASS_PURPOSE_LABELS: Record<ClassPurpose, string> = {
  THEORY: "Theory",
  REVISION: "Revision",
  PAPER_CLASS: "Paper Class",
  PRACTICAL: "Practical",
};

export const CLASS_PURPOSE_ORDER: ClassPurpose[] = ["THEORY", "REVISION", "PAPER_CLASS", "PRACTICAL"];

export const CURRICULUM_LABELS: Record<Curriculum, string> = {
  SRI_LANKAN_NATIONAL: "Sri Lankan National",
  CAMBRIDGE: "Cambridge",
  PEARSON_EDEXCEL: "Pearson Edexcel",
  OTHER: "Other",
};

export const CURRICULUM_ORDER: Curriculum[] = ["SRI_LANKAN_NATIONAL", "CAMBRIDGE", "PEARSON_EDEXCEL", "OTHER"];

export const MEDIUM_LABELS: Record<Medium, string> = {
  SINHALA: "Sinhala",
  ENGLISH: "English",
  TAMIL: "Tamil",
};

export const MEDIUM_ORDER: Medium[] = ["SINHALA", "ENGLISH", "TAMIL"];

export const LEVEL_LABELS: Record<TuitionLevel, string> = {
  PRIMARY: "Primary",
  GRADE_6_9: "Grade 6-9",
  OL: "O/L",
  AL: "A/L",
  IGCSE: "IGCSE",
  AS_LEVEL: "AS Level",
  A_LEVEL: "A Level",
  UNIVERSITY: "University",
  PROFESSIONAL: "Professional",
};

export const LEVEL_ORDER: TuitionLevel[] = [
  "PRIMARY",
  "GRADE_6_9",
  "OL",
  "AL",
  "IGCSE",
  "AS_LEVEL",
  "A_LEVEL",
  "UNIVERSITY",
  "PROFESSIONAL",
];

export const TEACHER_PROFILE_TYPE_LABELS: Record<TeacherProfileType, string> = {
  TEACHER: "Teacher",
  INSTITUTE: "Institute",
};

export const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: "Monday",
  TUESDAY: "Tuesday",
  WEDNESDAY: "Wednesday",
  THURSDAY: "Thursday",
  FRIDAY: "Friday",
  SATURDAY: "Saturday",
  SUNDAY: "Sunday",
};

export const DAY_SHORT_LABELS: Record<DayOfWeek, string> = {
  MONDAY: "Mon",
  TUESDAY: "Tue",
  WEDNESDAY: "Wed",
  THURSDAY: "Thu",
  FRIDAY: "Fri",
  SATURDAY: "Sat",
  SUNDAY: "Sun",
};

// Monday-first, used to sort sessions into a "sensible order" per class-schedule UX.
export const DAY_ORDER: DayOfWeek[] = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
