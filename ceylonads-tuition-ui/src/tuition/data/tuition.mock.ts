import type { ClassFormat, ClassPurpose, Curriculum, DayOfWeek, DeliveryMode, DiscoveryTheme, Medium, TeacherProfile, TuitionLevel } from "../model/tuition";

// Templates hold every piece of tuition metadata the real CeylonAds backend does not support yet.
// They are pure data - deriving DeliveryMode/ClassFormat/ClassPurpose unions from `sessions` +
// `homeVisit` happens in mockTuitionRepository.ts so the two never drift out of sync.
//
// Curriculum, level and mediums are all left `undefined` on templates for non-academic activities
// (dancing, music, chess...) - that absence is the point, not an oversight: the UI must render
// those listings without forcing empty "N/A" rows.
//
// Physical sessions do not carry a location here: mockTuitionRepository resolves
// `locationId`/`locationName`/`venueName` at generation time from the real ad's own locations (or
// a deterministic nearby real location), so schedules always reference real CeylonAds location
// data rather than an invented place name.
//
// shortBio deliberately avoids naming a specific subject/activity - template selection is biased
// by keyword hints but falls back to a hash pick, so a bio must read sensibly attached to any ad.

export interface SessionPattern {
  dayOfWeek: DayOfWeek;
  /** 24h "HH:mm" */
  startTime: string;
  /** 24h "HH:mm" */
  endTime: string;
  deliveryMode: Exclude<DeliveryMode, "HOME_VISIT">;
  classFormats: ClassFormat[];
  /** Often empty for non-academic activities. */
  classPurposes?: ClassPurpose[];
  venueName?: string;
  /** Resolve this session to a second, different real location than the ad's primary one. */
  useSecondaryLocation?: boolean;
}

export interface TuitionTemplate {
  id: string;
  /** Mock fallback learning-area label, used only when the real ad has no "subject" attribute. */
  subjectLabel: string;
  level?: TuitionLevel;
  curriculum?: Curriculum;
  mediums?: Medium[];
  theme?: DiscoveryTheme;
  targetExamYear?: string;
  sessions: SessionPattern[];
  homeVisit?: { available: boolean; classFormats: ClassFormat[] };
  teacher: TeacherProfile;
  /** Lowercase substrings matched against the real ad's grade attribute + title to bias template selection. */
  hints: string[];
}

export const TUITION_TEMPLATES: TuitionTemplate[] = [
  // ---- Sri Lankan National curriculum -------------------------------------------------------
  {
    id: "al-combined-maths-group",
    subjectLabel: "Combined Mathematics",
    level: "AL",
    curriculum: "SRI_LANKAN_NATIONAL",
    mediums: ["ENGLISH"],
    targetExamYear: "2027 A/L",
    sessions: [
      { dayOfWeek: "SATURDAY", startTime: "08:00", endTime: "11:00", deliveryMode: "PHYSICAL", classFormats: ["GROUP"], classPurposes: ["THEORY"] },
      { dayOfWeek: "WEDNESDAY", startTime: "19:00", endTime: "21:00", deliveryMode: "ONLINE", classFormats: ["GROUP"], classPurposes: ["REVISION"] },
    ],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Strong focus on paper-based exam technique, with weekly group revision.",
      qualifications: ["BSc (Hons) Mathematics - University of Colombo", "PGDE"],
      experienceYears: 12,
      teachingSince: 2014,
    },
    hints: ["a/l", "advanced level", "12", "13"],
  },
  {
    id: "al-physics-individual-homevisit",
    subjectLabel: "Physics",
    level: "AL",
    curriculum: "SRI_LANKAN_NATIONAL",
    mediums: ["ENGLISH"],
    targetExamYear: "2027 A/L",
    sessions: [{ dayOfWeek: "TUESDAY", startTime: "16:00", endTime: "18:00", deliveryMode: "PHYSICAL", classFormats: ["INDIVIDUAL"], classPurposes: ["PAPER_CLASS"] }],
    homeVisit: { available: true, classFormats: ["INDIVIDUAL"] },
    teacher: {
      profileType: "TEACHER",
      shortBio: "One-to-one tuition with structured paper classes at the student's convenience.",
      qualifications: ["BSc Physics - University of Peradeniya"],
      experienceYears: 8,
      teachingSince: 2018,
    },
    hints: ["a/l", "advanced level", "12", "13"],
  },
  {
    id: "ol-maths-mass-institute",
    subjectLabel: "Mathematics",
    level: "OL",
    curriculum: "SRI_LANKAN_NATIONAL",
    mediums: ["SINHALA", "ENGLISH"],
    targetExamYear: "2026 O/L",
    sessions: [
      { dayOfWeek: "SUNDAY", startTime: "09:00", endTime: "12:00", deliveryMode: "PHYSICAL", classFormats: ["MASS_CLASS"], classPurposes: ["THEORY"], venueName: "Main Hall" },
      { dayOfWeek: "THURSDAY", startTime: "17:00", endTime: "19:00", deliveryMode: "PHYSICAL", classFormats: ["MASS_CLASS"], classPurposes: ["REVISION"], venueName: "Branch Study Centre", useSecondaryLocation: true },
    ],
    teacher: {
      profileType: "INSTITUTE",
      shortBio: "A well-established mass class with a strong exam-results track record.",
      experienceYears: 15,
      teachingSince: 2011,
      affiliation: "Bright Minds Institute",
    },
    hints: ["o/l", "ordinary level", "10", "11"],
  },
  {
    id: "ol-science-group-hybrid",
    subjectLabel: "Science",
    level: "OL",
    curriculum: "SRI_LANKAN_NATIONAL",
    mediums: ["SINHALA"],
    targetExamYear: "2026 O/L",
    sessions: [
      { dayOfWeek: "MONDAY", startTime: "15:30", endTime: "17:30", deliveryMode: "PHYSICAL", classFormats: ["GROUP"], classPurposes: ["THEORY", "PRACTICAL"] },
      { dayOfWeek: "FRIDAY", startTime: "19:00", endTime: "20:30", deliveryMode: "ONLINE", classFormats: ["GROUP"], classPurposes: ["REVISION"] },
    ],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Combines physical group classes with an online revision session each week.",
      qualifications: ["BSc Applied Sciences - University of Sri Jayewardenepura"],
      experienceYears: 6,
      teachingSince: 2020,
    },
    hints: ["o/l", "ordinary level", "10", "11"],
  },
  {
    id: "al-chemistry-theory-revision",
    subjectLabel: "Chemistry",
    level: "AL",
    curriculum: "SRI_LANKAN_NATIONAL",
    mediums: ["ENGLISH"],
    targetExamYear: "2026 A/L",
    sessions: [
      { dayOfWeek: "SATURDAY", startTime: "13:00", endTime: "16:00", deliveryMode: "PHYSICAL", classFormats: ["GROUP"], classPurposes: ["THEORY"] },
      { dayOfWeek: "SATURDAY", startTime: "16:15", endTime: "17:45", deliveryMode: "PHYSICAL", classFormats: ["GROUP"], classPurposes: ["REVISION", "PAPER_CLASS"] },
    ],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Back-to-back theory and paper-class sessions every Saturday.",
      qualifications: ["BSc (Hons) Chemistry - University of Kelaniya"],
      experienceYears: 10,
      teachingSince: 2016,
    },
    hints: ["a/l", "advanced level", "12", "13"],
  },
  {
    id: "al-biology-online-individual",
    subjectLabel: "Biology",
    level: "AL",
    curriculum: "SRI_LANKAN_NATIONAL",
    mediums: ["ENGLISH"],
    targetExamYear: "2027 A/L",
    sessions: [{ dayOfWeek: "WEDNESDAY", startTime: "18:00", endTime: "19:30", deliveryMode: "ONLINE", classFormats: ["INDIVIDUAL"] }],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Fully online, one-to-one tuition for busy schedules.",
      qualifications: ["BSc Biological Sciences - University of Colombo"],
      experienceYears: 5,
      teachingSince: 2021,
    },
    hints: ["a/l", "advanced level", "12", "13"],
  },
  {
    id: "ol-english-small-group",
    subjectLabel: "English",
    level: "OL",
    curriculum: "SRI_LANKAN_NATIONAL",
    mediums: ["ENGLISH"],
    targetExamYear: "2026 O/L",
    sessions: [{ dayOfWeek: "TUESDAY", startTime: "16:00", endTime: "17:30", deliveryMode: "PHYSICAL", classFormats: ["SMALL_GROUP"], classPurposes: ["THEORY"] }],
    homeVisit: { available: true, classFormats: ["INDIVIDUAL", "SMALL_GROUP"] },
    teacher: {
      profileType: "TEACHER",
      shortBio: "Small-group classes with home-visit tuition also available.",
      experienceYears: 9,
      teachingSince: 2017,
    },
    hints: ["o/l", "ordinary level", "9", "10", "11"],
  },
  {
    id: "primary-maths-individual-homevisit",
    subjectLabel: "Mathematics",
    level: "PRIMARY",
    mediums: ["ENGLISH"],
    sessions: [{ dayOfWeek: "SATURDAY", startTime: "09:00", endTime: "10:00", deliveryMode: "PHYSICAL", classFormats: ["INDIVIDUAL"] }],
    homeVisit: { available: true, classFormats: ["INDIVIDUAL"] },
    teacher: {
      profileType: "TEACHER",
      shortBio: "Patient, individual attention for young learners building strong fundamentals.",
      qualifications: ["Diploma in Primary Education"],
      experienceYears: 7,
      teachingSince: 2019,
    },
    hints: ["primary", "grade 1", "grade 2", "grade 3", "grade 4", "grade 5"],
  },
  {
    id: "grade6-9-science-small-group-homevisit",
    subjectLabel: "Science",
    level: "GRADE_6_9",
    sessions: [{ dayOfWeek: "FRIDAY", startTime: "15:00", endTime: "16:30", deliveryMode: "PHYSICAL", classFormats: ["SMALL_GROUP"], classPurposes: ["THEORY"] }],
    homeVisit: { available: true, classFormats: ["INDIVIDUAL", "SMALL_GROUP"] },
    teacher: {
      profileType: "TEACHER",
      shortBio: "Small-group tuition for Grades 6-9, with home visits available on request.",
      qualifications: ["BSc Education - Open University of Sri Lanka"],
      experienceYears: 6,
      teachingSince: 2020,
    },
    hints: ["grade 6", "grade 7", "grade 8", "grade 9"],
  },
  {
    id: "ict-online-mass",
    subjectLabel: "ICT",
    level: "OL",
    curriculum: "SRI_LANKAN_NATIONAL",
    mediums: ["ENGLISH"],
    targetExamYear: "2026 O/L",
    sessions: [{ dayOfWeek: "SUNDAY", startTime: "10:00", endTime: "12:00", deliveryMode: "ONLINE", classFormats: ["MASS_CLASS"], classPurposes: ["THEORY"] }],
    teacher: {
      profileType: "INSTITUTE",
      shortBio: "Online classes covering the full syllabus with weekly paper practice.",
      experienceYears: 11,
      teachingSince: 2015,
      affiliation: "TechEd Learning Centre",
    },
    hints: ["o/l", "ordinary level", "10", "11", "ict"],
  },
  {
    id: "accounting-institute-group",
    subjectLabel: "Accounting",
    level: "OL",
    curriculum: "SRI_LANKAN_NATIONAL",
    mediums: ["SINHALA", "ENGLISH"],
    targetExamYear: "2026 O/L",
    sessions: [
      { dayOfWeek: "MONDAY", startTime: "17:00", endTime: "19:00", deliveryMode: "PHYSICAL", classFormats: ["GROUP"], classPurposes: ["THEORY"], venueName: "Institute Branch 1" },
      { dayOfWeek: "WEDNESDAY", startTime: "17:00", endTime: "19:00", deliveryMode: "PHYSICAL", classFormats: ["GROUP"], classPurposes: ["REVISION"], venueName: "Institute Branch 2", useSecondaryLocation: true },
    ],
    teacher: {
      profileType: "INSTITUTE",
      shortBio: "Two-branch institute with dedicated theory and revision streams.",
      experienceYears: 18,
      teachingSince: 2008,
      affiliation: "Ledger Point Institute",
    },
    hints: ["o/l", "ordinary level", "accounting", "10", "11"],
  },

  // ---- Cambridge ------------------------------------------------------------------------------
  {
    id: "cambridge-igcse-maths-group",
    subjectLabel: "Mathematics",
    level: "IGCSE",
    curriculum: "CAMBRIDGE",
    mediums: ["ENGLISH"],
    sessions: [{ dayOfWeek: "WEDNESDAY", startTime: "19:00", endTime: "21:00", deliveryMode: "ONLINE", classFormats: ["GROUP"], classPurposes: ["THEORY"] }],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Covers the full syllabus with past-paper practice built into every session.",
      qualifications: ["BSc (Hons) Mathematics - University of Colombo"],
      experienceYears: 9,
      teachingSince: 2017,
    },
    hints: ["cambridge", "igcse"],
  },
  {
    id: "cambridge-alevel-economics-hybrid",
    subjectLabel: "Economics",
    level: "A_LEVEL",
    curriculum: "CAMBRIDGE",
    mediums: ["ENGLISH"],
    sessions: [{ dayOfWeek: "SATURDAY", startTime: "14:00", endTime: "16:30", deliveryMode: "HYBRID", classFormats: ["GROUP"], classPurposes: ["THEORY", "REVISION"], venueName: "City Campus" }],
    teacher: {
      profileType: "INSTITUTE",
      shortBio: "Attend physically or join online for the same live session.",
      experienceYears: 13,
      teachingSince: 2013,
      affiliation: "Horizon Cambridge Centre",
    },
    hints: ["cambridge", "a level", "as level"],
  },

  // ---- Pearson Edexcel ------------------------------------------------------------------------
  {
    id: "edexcel-igcse-online",
    subjectLabel: "Mathematics",
    level: "IGCSE",
    curriculum: "PEARSON_EDEXCEL",
    mediums: ["ENGLISH"],
    sessions: [{ dayOfWeek: "MONDAY", startTime: "18:30", endTime: "20:00", deliveryMode: "ONLINE", classFormats: ["GROUP"], classPurposes: ["THEORY"] }],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Structured around past papers and mark-scheme walkthroughs.",
      qualifications: ["BSc Mathematics - University of Moratuwa"],
      experienceYears: 7,
      teachingSince: 2019,
    },
    hints: ["edexcel", "igcse"],
  },
  {
    id: "edexcel-alevel-business",
    subjectLabel: "Business Studies",
    level: "A_LEVEL",
    curriculum: "PEARSON_EDEXCEL",
    mediums: ["ENGLISH"],
    sessions: [{ dayOfWeek: "THURSDAY", startTime: "17:00", endTime: "19:00", deliveryMode: "PHYSICAL", classFormats: ["SMALL_GROUP"], classPurposes: ["THEORY", "REVISION"] }],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Small-group sessions with case-study discussion and essay feedback.",
      qualifications: ["BBA - University of Colombo"],
      experienceYears: 8,
      teachingSince: 2018,
    },
    hints: ["edexcel", "a level"],
  },

  // ---- Professional ----------------------------------------------------------------------------
  {
    id: "professional-accounting-paperclass",
    subjectLabel: "CIMA / ACCA",
    level: "PROFESSIONAL",
    mediums: ["ENGLISH"],
    sessions: [{ dayOfWeek: "SUNDAY", startTime: "08:30", endTime: "12:30", deliveryMode: "HYBRID", classFormats: ["GROUP"], classPurposes: ["REVISION", "PAPER_CLASS"], venueName: "City Campus" }],
    teacher: {
      profileType: "INSTITUTE",
      shortBio: "Revision and paper-class sessions, attend physically or join online.",
      experienceYears: 14,
      teachingSince: 2012,
      affiliation: "Apex Professional Studies",
    },
    hints: ["professional", "cima", "acca", "university"],
  },

  // ---- Language / exam preparation - no fixed academic level -----------------------------------
  {
    id: "spoken-english-group",
    subjectLabel: "Spoken English",
    theme: "LANGUAGE",
    sessions: [{ dayOfWeek: "TUESDAY", startTime: "18:00", endTime: "19:30", deliveryMode: "PHYSICAL", classFormats: ["GROUP"] }],
    homeVisit: { available: true, classFormats: ["INDIVIDUAL"] },
    teacher: {
      profileType: "TEACHER",
      shortBio: "Conversation-first sessions building everyday speaking confidence.",
      experienceYears: 6,
      teachingSince: 2020,
    },
    hints: ["spoken english", "all levels", "conversation"],
  },
  {
    id: "ielts-online-individual",
    subjectLabel: "IELTS",
    theme: "LANGUAGE",
    targetExamYear: "IELTS",
    sessions: [{ dayOfWeek: "THURSDAY", startTime: "19:00", endTime: "20:30", deliveryMode: "ONLINE", classFormats: ["INDIVIDUAL"], classPurposes: ["REVISION"] }],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Focused coaching covering every module, with regular mock-test reviews.",
      qualifications: ["CELTA"],
      experienceYears: 9,
      teachingSince: 2017,
    },
    hints: ["ielts"],
  },

  // ---- Skills ------------------------------------------------------------------------------------
  {
    id: "skill-coding-online-practical",
    subjectLabel: "Coding",
    theme: "SKILL",
    sessions: [{ dayOfWeek: "SATURDAY", startTime: "10:00", endTime: "11:30", deliveryMode: "ONLINE", classFormats: ["INDIVIDUAL"], classPurposes: ["PRACTICAL"] }],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Practical, project-based classes taught entirely online.",
      experienceYears: 4,
      teachingSince: 2022,
    },
    hints: ["coding", "programming", "robotics", "skill"],
  },
  {
    id: "chess-small-group-physical",
    subjectLabel: "Chess",
    theme: "SKILL",
    sessions: [{ dayOfWeek: "SATURDAY", startTime: "10:00", endTime: "11:00", deliveryMode: "PHYSICAL", classFormats: ["SMALL_GROUP"] }],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Small groups so every student gets time on the board with the coach.",
      experienceYears: 5,
      teachingSince: 2021,
    },
    hints: ["chess"],
  },
  {
    id: "public-speaking-group",
    subjectLabel: "Public Speaking",
    theme: "SKILL",
    sessions: [{ dayOfWeek: "SUNDAY", startTime: "16:00", endTime: "17:30", deliveryMode: "PHYSICAL", classFormats: ["GROUP"], classPurposes: ["PRACTICAL"] }],
    teacher: {
      profileType: "INSTITUTE",
      shortBio: "Weekly practice rounds with recorded feedback.",
      experienceYears: 10,
      teachingSince: 2016,
      affiliation: "Speak Up Academy",
    },
    hints: ["public speaking", "debate", "toastmasters"],
  },

  // ---- Arts / dancing / music - no curriculum, no level, usually no medium --------------------
  {
    id: "dancing-kandyan-group-physical",
    subjectLabel: "Kandyan Dancing",
    theme: "ARTS",
    sessions: [{ dayOfWeek: "SATURDAY", startTime: "09:00", endTime: "10:30", deliveryMode: "PHYSICAL", classFormats: ["GROUP"], classPurposes: ["PRACTICAL"] }],
    teacher: {
      profileType: "INSTITUTE",
      shortBio: "Traditional technique taught in a structured group setting.",
      experienceYears: 20,
      teachingSince: 2006,
      affiliation: "Natya Kala Academy",
    },
    hints: ["dancing", "dance", "kandyan", "bharatanatyam"],
  },
  {
    id: "music-general-group",
    subjectLabel: "Music",
    theme: "ARTS",
    sessions: [{ dayOfWeek: "FRIDAY", startTime: "16:00", endTime: "17:00", deliveryMode: "PHYSICAL", classFormats: ["GROUP"], classPurposes: ["PRACTICAL"] }],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Group sessions covering theory basics alongside hands-on practice.",
      experienceYears: 8,
      teachingSince: 2018,
    },
    hints: ["music", "eastern music", "western music", "singing"],
  },
  {
    id: "guitar-individual-homevisit",
    subjectLabel: "Guitar",
    theme: "ARTS",
    sessions: [{ dayOfWeek: "WEDNESDAY", startTime: "17:00", endTime: "18:00", deliveryMode: "PHYSICAL", classFormats: ["INDIVIDUAL"], classPurposes: ["PRACTICAL"] }],
    homeVisit: { available: true, classFormats: ["INDIVIDUAL"] },
    teacher: {
      profileType: "TEACHER",
      shortBio: "One-to-one lessons paced to the student, at home or in person.",
      experienceYears: 11,
      teachingSince: 2015,
    },
    hints: ["guitar"],
  },
  {
    id: "piano-individual-physical",
    subjectLabel: "Piano",
    theme: "ARTS",
    sessions: [{ dayOfWeek: "TUESDAY", startTime: "15:00", endTime: "16:00", deliveryMode: "PHYSICAL", classFormats: ["INDIVIDUAL"], classPurposes: ["PRACTICAL"] }],
    teacher: {
      profileType: "TEACHER",
      shortBio: "Structured grade-exam preparation alongside general practice.",
      qualifications: ["Trinity College London - Grade 8"],
      experienceYears: 9,
      teachingSince: 2017,
    },
    hints: ["piano"],
  },
];
