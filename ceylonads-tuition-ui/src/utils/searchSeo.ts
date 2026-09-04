// Centralized SEO rules for the /classes search page - the single source of truth for which
// subject/deliveryMode/location combinations are "SEO-worthy" (indexable, in the sitemap-shaped
// sense) versus which are ordinary search functionality that should stay noindex. Deliberately not
// scattered across page components: ClassesPage (and ceylonads-api's TuitionSitemapService,
// independently, for the sitemap) both need the exact same subject/deliveryMode/location shapes.
//
// Approved indexable shapes: no filters, subject only, deliveryMode only, subject+deliveryMode,
// subject+location. Anything else (level/curriculum/medium/classFormat/classPurpose/q/price
// filters present, a non-first page, or subject+deliveryMode+location together) is noindex, and
// canonicalizes down to the closest approved shape instead of its own exact combination.

// A few subjects read more naturally in Sri Lankan English than their raw backend option label
// (e.g. "Mathematics" -> "Maths") - everything else falls back to a humanized version of the
// enum code, so a not-yet-listed subject still gets a readable title instead of a raw code.
const SUBJECT_LABEL_OVERRIDES: Record<string, string> = {
  MATHEMATICS: "Maths",
  COMBINED_MATHEMATICS: "Combined Maths",
  SPOKEN_ENGLISH: "Spoken English",
  ICT: "ICT",
  IELTS: "IELTS",
};

function humanize(code: string): string {
  return code
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((word) => word[0].toUpperCase() + word.slice(1))
    .join(" ");
}

export function subjectLabel(subjectCode: string): string {
  return SUBJECT_LABEL_OVERRIDES[subjectCode] ?? humanize(subjectCode);
}

// PHYSICAL/BOTH have no natural single-word SEO adjective ("Physical Classes" / "Online & Physical
// Classes" don't read like real search phrases), so titles/H1s for those modes fall back to the
// subject-only (or generic) form instead of forcing an awkward adjective.
export function deliveryAdjective(mode: string): string | null {
  if (mode === "ONLINE") return "Online";
  if (mode === "HOME_VISIT") return "Home Visit";
  return null;
}

const BLOCKING_PARAM_KEYS = [
  "q",
  "category",
  "level",
  "curriculum",
  "medium",
  "classFormat",
  "classPurpose",
  "minPrice",
  "maxPrice",
] as const;

export interface SearchSeoDecision {
  indexable: boolean;
  canonicalSubject?: string;
  canonicalDeliveryMode?: string;
  canonicalLocation?: string;
}

// sort/page are deliberately ignored here except for "is this the first page" - see CLAUDE.md's
// canonical-normalization rule: they never affect the canonical target, and sort alone never
// blocks indexability, but a non-first page always does (only the first page of a result set is
// ever worth indexing).
export function decideSearchSeo(params: URLSearchParams): SearchSeoDecision {
  const subject = params.get("subject") || undefined;
  const deliveryMode = params.get("deliveryMode") || undefined;
  const location = params.get("location") || undefined;
  const page = Number(params.get("page"));
  const isFirstPage = !Number.isFinite(page) || page <= 0;
  const hasBlockingParam = BLOCKING_PARAM_KEYS.some((key) => !!params.get(key));

  const isApprovedShape =
    (!!subject && !!deliveryMode && !location) ||
    (!!subject && !!location && !deliveryMode) ||
    (!!subject && !deliveryMode && !location) ||
    (!subject && !!deliveryMode && !location) ||
    (!subject && !deliveryMode && !location);

  // Canonicalizes toward the strongest approved signal available (subject, then deliveryMode) when
  // the current combination isn't one of the approved shapes itself (e.g. all three dimensions
  // present at once) - never links to a page shape the sitemap/indexability rule wouldn't approve.
  let canonicalSubject: string | undefined;
  let canonicalDeliveryMode: string | undefined;
  let canonicalLocation: string | undefined;
  if (isApprovedShape) {
    canonicalSubject = subject;
    canonicalDeliveryMode = deliveryMode;
    canonicalLocation = location;
  } else if (subject) {
    canonicalSubject = subject;
  } else if (deliveryMode) {
    canonicalDeliveryMode = deliveryMode;
  }

  return {
    indexable: isFirstPage && !hasBlockingParam && isApprovedShape,
    canonicalSubject,
    canonicalDeliveryMode,
    canonicalLocation,
  };
}

export function buildCanonicalSearchPath(
  decision: Pick<SearchSeoDecision, "canonicalSubject" | "canonicalDeliveryMode" | "canonicalLocation">,
): string {
  const params = new URLSearchParams();
  // Fixed order (subject, deliveryMode, location) regardless of the order filters were applied in,
  // so the same logical page never produces two different canonical URLs.
  if (decision.canonicalSubject) params.set("subject", decision.canonicalSubject);
  if (decision.canonicalDeliveryMode) params.set("deliveryMode", decision.canonicalDeliveryMode);
  if (decision.canonicalLocation) params.set("location", decision.canonicalLocation);
  const query = params.toString();
  return query ? `/classes?${query}` : "/classes";
}

export interface SearchSeoContent {
  title: string;
  description: string;
  h1: string;
  /** Short visible SEO intro copy - only meant to be rendered for indexable combinations. */
  intro: string;
}

export function buildSearchSeoContent(opts: {
  subjectCode?: string;
  deliveryMode?: string;
  locationName?: string;
}): SearchSeoContent {
  const subject = opts.subjectCode ? subjectLabel(opts.subjectCode) : null;
  const adjective = opts.deliveryMode ? deliveryAdjective(opts.deliveryMode) : null;
  const place = opts.locationName ?? "Sri Lanka";

  const prefix = [adjective, subject].filter(Boolean).join(" ");
  const title = prefix ? `${prefix} Classes & Tuition in ${place}` : `Tuition Classes in Sri Lanka`;
  const h1 = prefix ? `${prefix} Classes in ${place}` : `Tuition Classes in Sri Lanka`;

  if (!subject && !adjective && !opts.locationName) {
    return {
      title,
      h1,
      description:
        "Find tuition classes, tutors and panthi across Sri Lanka. Search online, physical and home-visit classes for English, maths, science, chess and more.",
      intro:
        "Browse tuition classes, tutors and panthi across Sri Lanka. Search by subject, delivery mode and district to find online, physical and home-visit classes, then contact the tutor directly through ezClass.",
    };
  }

  const subjectWord = subject ? subject.toLowerCase() : "tuition";
  const adjectiveWord = adjective ? `${adjective.toLowerCase()} ` : "";

  const description = opts.locationName
    ? `Find ${subjectWord} classes, tuition and panthi in ${opts.locationName}. Browse local tutors, online options and ${adjectiveWord}${subjectWord} classes on ezClass.`
    : `Find ${adjectiveWord}${subjectWord} classes, tuition and panthi from tutors across Sri Lanka. Compare class details, delivery options and tutors on ezClass.`;

  const intro = `Looking for ${adjectiveWord}${subjectWord} classes${
    opts.locationName ? ` in ${opts.locationName}` : " in Sri Lanka"
  }? Browse ${subjectWord} tuition and ${adjectiveWord}panthi from tutors ${
    opts.locationName ? `in ${opts.locationName}` : "across the country"
  }. Compare available classes, fees, teaching formats and tutor details, then contact the tutor directly through ezClass.`;

  return { title, description, h1, intro };
}
