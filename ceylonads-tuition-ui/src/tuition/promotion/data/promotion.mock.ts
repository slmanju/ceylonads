import type { TuitionPromotion } from "../model/promotion";

// Realistic mock tuition promotion inventory covering every placement type (see tuition-promotion
// spec section "MOCK PROMOTION DATA"). No promotion here duplicates a full tuition ad or profile -
// each is a short, self-contained pitch with a typed target (see PromotionTarget). Targets use
// EXTERNAL with a real in-app route (a filtered /classes search, /online-classes, etc.) rather than
// a guessed backend ad slug, since the mock dataset has no guaranteed real ad to link to - a real
// backend-driven promotion would instead set an AD/TEACHER_PROFILE/INSTITUTE_PROFILE target with an
// actual ad slug. activeFrom/activeTo intentionally bracket a wide window so the prototype reads as
// "currently running" regardless of exact demo date.
const ACTIVE_FROM = "2026-01-01";
const ACTIVE_TO = "2026-12-31";

export const TUITION_PROMOTIONS: TuitionPromotion[] = [
  // ---- Search page top banner (premium, self-serve CTA - see PromotionBanner) ------------------
  {
    id: "search-top-banner-promote",
    placementType: "TUITION_SEARCH_TOP_BANNER",
    label: "SPONSORED",
    title: "Promote your tuition class",
    subtitle: "Featured on top of search results · Reach students across Sri Lanka",
    ctaLabel: "Promote Now",
    target: { type: "EXTERNAL", url: "/post-ad" },
    displayOrder: 1,
    activeFrom: ACTIVE_FROM,
    activeTo: ACTIVE_TO,
  },

  // ---- Search page sidebar (see PromotionSidebar) - one card per named slot, distinct target
  // types so the sidebar demonstrates a Sponsored Class, a Sponsored Tutor and a Sponsored
  // Institute side by side. ----------------------------------------------------------------------
  {
    id: "search-sidebar-top-ol-science",
    placementType: "TUITION_SEARCH_SIDEBAR_TOP",
    label: "SPONSORED",
    title: "2026 O/L Science Revision Program",
    subtitle: "Theory + practical revision · O/L · Colombo",
    ctaLabel: "View Class",
    target: { type: "AD", url: "/classes?category=school-tuition&level=OL" },
    displayOrder: 1,
    activeFrom: ACTIVE_FROM,
    activeTo: ACTIVE_TO,
  },
  {
    id: "search-sidebar-middle-mrs-perera",
    placementType: "TUITION_SEARCH_SIDEBAR_MIDDLE",
    label: "SPONSORED",
    title: "Mrs. Perera — Cambridge & Edexcel Maths",
    subtitle: "IGCSE & AS/A Level Mathematics · Online & Physical · Colombo",
    ctaLabel: "View Tutor",
    target: { type: "TEACHER_PROFILE", url: "/classes?category=school-tuition&curriculum=CAMBRIDGE" },
    displayOrder: 1,
    activeFrom: ACTIVE_FROM,
    activeTo: ACTIVE_TO,
  },
  {
    id: "search-sidebar-bottom-bright-minds",
    placementType: "TUITION_SEARCH_SIDEBAR_BOTTOM",
    label: "SPONSORED",
    title: "Bright Minds Institute",
    subtitle: "A/L Combined Mathematics · Sri Lankan National · Colombo",
    ctaLabel: "View Institute",
    target: { type: "INSTITUTE_PROFILE", url: "/classes?category=school-tuition&level=AL&curriculum=SRI_LANKAN_NATIONAL" },
    displayOrder: 1,
    activeFrom: ACTIVE_FROM,
    activeTo: ACTIVE_TO,
  },

  // ---- Detail page side promotion ----------------------------------------------------------------
  {
    id: "detail-side-bright-minds",
    placementType: "TUITION_DETAIL_SIDE",
    label: "SPONSORED",
    title: "Bright Minds Institute",
    subtitle: "Book a free trial class · A/L Combined Mathematics",
    ctaLabel: "Book Free Trial",
    target: { type: "INSTITUTE_PROFILE", url: "/classes?category=school-tuition&level=AL" },
    displayOrder: 1,
    eligibility: { profileType: "INSTITUTE", levels: ["AL"], subjects: ["combined mathematics", "maths", "mathematics"] },
    activeFrom: ACTIVE_FROM,
    activeTo: ACTIVE_TO,
  },
  {
    id: "detail-side-speakwell",
    placementType: "TUITION_DETAIL_SIDE",
    label: "SPONSORED",
    title: "SpeakWell Online English",
    subtitle: "Free spoken-English level assessment this week",
    ctaLabel: "Get Free Assessment",
    target: { type: "TEACHER_PROFILE", url: "/online-classes" },
    displayOrder: 2,
    eligibility: { profileType: "TEACHER", subjects: ["english", "spoken english", "ielts"] },
    activeFrom: ACTIVE_FROM,
    activeTo: ACTIVE_TO,
  },
  {
    id: "detail-side-ol-science",
    placementType: "TUITION_DETAIL_SIDE",
    label: "SPONSORED",
    title: "O/L Science Revision Program — 2026",
    subtitle: "Theory + practical revision for the 2026 O/L batch",
    ctaLabel: "See Program",
    target: { type: "INSTITUTE_PROFILE", url: "/classes?category=school-tuition&level=OL" },
    displayOrder: 3,
    eligibility: { profileType: "INSTITUTE", levels: ["OL"], subjects: ["science"] },
    activeFrom: ACTIVE_FROM,
    activeTo: ACTIVE_TO,
  },

  // ---- Detail page banner ------------------------------------------------------------------------
  {
    id: "detail-banner-exam-webinar",
    placementType: "TUITION_DETAIL_BANNER",
    label: "SPONSORED",
    title: "Free Exam Strategy Webinar — O/L & A/L 2026",
    subtitle: "Live online session with past-paper marking tips",
    ctaLabel: "Reserve a Seat",
    target: { type: "EXTERNAL", url: "/classes?category=school-tuition" },
    displayOrder: 1,
    eligibility: { levels: ["OL", "AL"] },
    activeFrom: ACTIVE_FROM,
    activeTo: ACTIVE_TO,
  },
];
