import { getAd, getCategoryFeaturedAds, searchAds } from "../../api/adsApi";
import type {
  AdResponse,
  LocationResponse,
  PageResponse,
  TuitionClassCardResponse,
  TuitionClassDetailResponse,
  TuitionFeaturedCardResponse,
  TuitionFilterMetadataResponse,
} from "../../types/api";
import { curriculumEnumFromValue, getAttrValue } from "../../utils/tuitionAttributes";
import { buildFeaturedCardFromAd, buildTuitionCardFromAd, buildTuitionDetailFromAd } from "./tuitionDetailFromAd";
import { TUITION_ROOT_SLUG } from "../../hooks/useTuitionCategories";
import type {
  ClassFormat,
  ClassPurpose,
  Curriculum,
  DeliveryMode,
  HomeVisitInfo,
  HomeVisitServiceArea,
  Medium,
  TuitionDetails,
  TuitionSession,
} from "../model/tuition";
import { CLASS_FORMAT_ORDER, CLASS_PURPOSE_ORDER, DELIVERY_MODE_ORDER } from "../model/labels";
import { TUITION_TEMPLATES, type SessionPattern, type TuitionTemplate } from "../data/tuition.mock";
import type { FeaturedTuitionQuery, TuitionRepository } from "./tuitionRepository";

// Deterministic djb2-style hash so the same ad always resolves to the same mock template and
// locations across renders/reloads without needing a persisted mapping table.
function hashString(value: string): number {
  let hash = 5381;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 33) ^ value.charCodeAt(i);
  }
  return Math.abs(hash);
}

// The real "curriculum" attribute (school-tuition only) already covers this dimension with its
// own controlled vocabulary. Map it onto our enum (see utils/tuitionAttributes.ts, also reused by
// ClassDetailPage's promotion-context mapping) so the merged view never contradicts real data and
// the mock template value is a true fallback, used only when no real attribute exists.
function realCurriculumOf(ad: AdResponse): Curriculum | undefined {
  return curriculumEnumFromValue(getAttrValue(ad, "curriculum"));
}

function resolveCurriculum(ad: AdResponse, template: TuitionTemplate): Curriculum | undefined {
  return realCurriculumOf(ad) ?? template.curriculum;
}

const MEDIUM_VALUES: readonly Medium[] = ["SINHALA", "ENGLISH", "TAMIL"];

function resolveMediums(ad: AdResponse, template: TuitionTemplate): Medium[] | undefined {
  const raw = getAttrValue(ad, "medium");
  if (raw) {
    const values = raw.split(",").filter((v): v is Medium => (MEDIUM_VALUES as string[]).includes(v));
    if (values.length > 0) return values;
  }
  return template.mediums;
}

// Matched against the real "grade" attribute (school-tuition only) AND the ad title, so
// non-school-tuition ads - which have no dynamic attributes at all - can still bias template
// selection via their title (e.g. "Kandyan Dancing Classes - Kandy").
function pickTemplate(ad: AdResponse, seed: number): TuitionTemplate {
  const haystack = `${getAttrValue(ad, "grade") ?? ""} ${ad.title}`.toLowerCase();
  const candidates = TUITION_TEMPLATES.filter((t) => t.hints.some((hint) => haystack.includes(hint)));
  let pool = candidates.length > 0 ? candidates : TUITION_TEMPLATES;

  // When the real curriculum attribute is known (exact, unlike free-text grade/title), prefer
  // candidates whose own curriculum agrees, so a Cambridge ad doesn't pull an Edexcel-flavoured
  // teacher bio/session just because both templates matched the same level hint.
  const realCurriculum = realCurriculumOf(ad);
  if (realCurriculum) {
    const curriculumMatched = pool.filter((t) => t.curriculum === realCurriculum);
    if (curriculumMatched.length > 0) pool = curriculumMatched;
  }

  return pool[seed % pool.length];
}

function cityLocations(locations: LocationResponse[]): LocationResponse[] {
  return locations.filter((l) => l.type === "CITY");
}

// Ads already carry their own real location(s); reuse the first one as the "primary" session
// location rather than inventing one for ads that have real location data.
function resolvePrimaryLocation(ad: AdResponse, locations: LocationResponse[], seed: number): LocationResponse | undefined {
  if (ad.locations.length > 0) return ad.locations[0];
  const pool = cityLocations(locations);
  return pool.length > 0 ? pool[seed % pool.length] : undefined;
}

function resolveSecondaryLocation(
  primary: LocationResponse | undefined,
  locations: LocationResponse[],
  seed: number,
): LocationResponse | undefined {
  const pool = cityLocations(locations).filter((l) => l.id !== primary?.id);
  if (pool.length === 0) return primary;
  const sameDistrict = primary ? pool.filter((l) => l.parentId === primary.parentId) : [];
  const source = sameDistrict.length > 0 ? sameDistrict : pool;
  return source[(seed + 7) % source.length];
}

function resolveServiceAreas(primary: LocationResponse | undefined, locations: LocationResponse[], seed: number): HomeVisitServiceArea[] {
  const pool = cityLocations(locations).filter((l) => l.id !== primary?.id);
  const sameDistrict = primary ? pool.filter((l) => l.parentId === primary.parentId) : [];
  const source = sameDistrict.length > 0 ? sameDistrict : pool;

  const areas: HomeVisitServiceArea[] = primary ? [{ locationId: primary.id, locationName: primary.name }] : [];
  const targetCount = Math.min(3, areas.length + source.length);
  const used = new Set(areas.map((a) => a.locationId));
  let i = seed;
  let attempts = 0;
  while (areas.length < targetCount && attempts < source.length * 2) {
    const loc = source[i % source.length];
    if (!used.has(loc.id)) {
      used.add(loc.id);
      areas.push({ locationId: loc.id, locationName: loc.name });
    }
    i++;
    attempts++;
  }
  return areas;
}

function buildSessions(
  adId: number,
  template: TuitionTemplate,
  primary: LocationResponse | undefined,
  secondary: LocationResponse | undefined,
): TuitionSession[] {
  return template.sessions.map((pattern: SessionPattern, index) => {
    const hasVenue = pattern.deliveryMode === "PHYSICAL" || pattern.deliveryMode === "HYBRID";
    const location = pattern.useSecondaryLocation ? secondary : primary;

    return {
      id: `${adId}-${template.id}-${index}`,
      deliveryMode: pattern.deliveryMode,
      locationId: hasVenue ? location?.id : undefined,
      locationName: hasVenue ? location?.name : undefined,
      venueName: hasVenue ? pattern.venueName : undefined,
      dayOfWeek: pattern.dayOfWeek,
      startTime: pattern.startTime,
      endTime: pattern.endTime,
      classFormats: pattern.classFormats,
      classPurposes: pattern.classPurposes ?? [],
    };
  });
}

function computeDeliveryModes(sessions: TuitionSession[], homeVisit?: HomeVisitInfo): DeliveryMode[] {
  const set = new Set<DeliveryMode>(sessions.map((s) => s.deliveryMode));
  if (homeVisit?.available) set.add("HOME_VISIT");
  return DELIVERY_MODE_ORDER.filter((m) => set.has(m));
}

function computeClassFormats(sessions: TuitionSession[], homeVisit?: HomeVisitInfo): ClassFormat[] {
  const set = new Set<ClassFormat>();
  for (const session of sessions) {
    for (const format of session.classFormats) set.add(format);
  }
  if (homeVisit?.available) {
    for (const format of homeVisit.classFormats) set.add(format);
  }
  return CLASS_FORMAT_ORDER.filter((f) => set.has(f));
}

function computeClassPurposes(sessions: TuitionSession[]): ClassPurpose[] {
  const set = new Set<ClassPurpose>();
  for (const session of sessions) {
    for (const purpose of session.classPurposes) set.add(purpose);
  }
  return CLASS_PURPOSE_ORDER.filter((p) => set.has(p));
}

export function generateTuitionDetails(ad: AdResponse, locations: LocationResponse[]): TuitionDetails {
  const seed = hashString(ad.slug || String(ad.id));
  const template = pickTemplate(ad, seed);
  const primary = resolvePrimaryLocation(ad, locations, seed);
  const secondary = resolveSecondaryLocation(primary, locations, seed);
  const sessions = buildSessions(ad.id, template, primary, secondary);

  const homeVisit: HomeVisitInfo | undefined = template.homeVisit?.available
    ? {
        available: true,
        serviceAreas: resolveServiceAreas(primary, locations, seed),
        classFormats: template.homeVisit.classFormats,
      }
    : undefined;

  return {
    adId: ad.id,
    subjectLabel: template.subjectLabel,
    theme: template.theme,
    level: template.level,
    curriculum: resolveCurriculum(ad, template),
    mediums: resolveMediums(ad, template),
    targetExamYear: template.targetExamYear,
    deliveryModes: computeDeliveryModes(sessions, homeVisit),
    classFormats: computeClassFormats(sessions, homeVisit),
    classPurposes: computeClassPurposes(sessions),
    sessions,
    homeVisit,
    teacher: template.teacher,
  };
}

// Generation is a pure, synchronous function of (ad, locations), so no caching is needed here -
// caching by ad id alone would go stale the moment `locations` finishes loading after an earlier
// call was made with an empty list.
export class MockTuitionRepository implements TuitionRepository {
  async getDetails(ad: AdResponse, locations: LocationResponse[]): Promise<TuitionDetails> {
    return generateTuitionDetails(ad, locations);
  }

  async getDetailsMap(ads: AdResponse[], locations: LocationResponse[]): Promise<Map<number, TuitionDetails>> {
    return new Map(ads.map((ad) => [ad.id, generateTuitionDetails(ad, locations)]));
  }

  // No mock dataset backs the class itself - the real ad still comes from the shared CeylonAds
  // backend (see CLAUDE.md: "reuse the shared ad-detail API"), reshaped into the lean tuition
  // detail contract so this mode is a faithful stand-in for the real /api/tuition/classes/{slug}.
  async getClassDetail(slug: string): Promise<TuitionClassDetailResponse> {
    const ad = await getAd(slug);
    return buildTuitionDetailFromAd(ad);
  }

  // Unreachable via the app's composed tuitionRepository (see tuitionApi.ts - getClassDetail/
  // getSimilarClasses always use the real endpoints), kept only to satisfy TuitionRepository.
  // No AbortSignal support in this stand-in since getAd/searchAds don't accept one.
  async getSimilarClasses(slug: string, size = 3): Promise<TuitionClassCardResponse[]> {
    const ad = await getAd(slug);
    const page = await searchAds({ category: ad.categorySlug, size: size + 1 });
    return page.content
      .filter((candidate) => candidate.id !== ad.id)
      .slice(0, size)
      .map(buildTuitionCardFromAd);
  }

  // Unreachable via the app's composed tuitionRepository (see tuitionApi.ts - getFeaturedTuition
  // always uses the real endpoint), kept only to satisfy TuitionRepository.
  async getFeaturedTuition({ size = 10 }: FeaturedTuitionQuery = {}): Promise<TuitionFeaturedCardResponse[]> {
    const ads = await getCategoryFeaturedAds(TUITION_ROOT_SLUG, size);
    return ads.map(buildFeaturedCardFromAd);
  }

  // Unreachable via the app's composed tuitionRepository (see tuitionApi.ts - getLatestClasses
  // always uses the real endpoint), kept only to satisfy TuitionRepository.
  async getLatestClasses(page = 0, size = 6): Promise<PageResponse<TuitionClassCardResponse>> {
    const result = await searchAds({ category: TUITION_ROOT_SLUG, page, size, sort: "newest" });
    return { ...result, content: result.content.map(buildTuitionCardFromAd) };
  }

  // Unreachable via the app's composed tuitionRepository (see tuitionApi.ts - getFilters always
  // uses the real endpoint), kept only to satisfy TuitionRepository. No mock master data exists to
  // derive filter options from, so this stands in with empty option lists.
  async getFilters(): Promise<TuitionFilterMetadataResponse> {
    return { subjects: [], levels: [], curricula: [], mediums: [], deliveryModes: [] };
  }
}
