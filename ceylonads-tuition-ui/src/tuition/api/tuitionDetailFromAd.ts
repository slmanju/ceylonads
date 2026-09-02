import type {
  AdAttributeResponse,
  AdResponse,
  TuitionAttributeValueLabel,
  TuitionClassCardResponse,
  TuitionClassDetailResponse,
  TuitionFeaturedCardResponse,
} from "../../types/api";

// Mock-mode stand-in for the real /api/tuition/classes/* endpoints: the generic AdResponse already
// carries resolved value+displayValue pairs for every attribute (see AdAttributeService server
// side), so this is a pure reshape - no separate mock dataset needed for it.
function findAttr(ad: AdResponse, key: string): AdAttributeResponse | undefined {
  return ad.attributes.find((a) => a.key === key);
}

function textValue(ad: AdResponse, key: string): string | null {
  const attr = findAttr(ad, key);
  return attr && attr.value.trim() !== "" ? attr.value : null;
}

// SELECT: single value+label pair.
function singleLabel(ad: AdResponse, key: string): TuitionAttributeValueLabel | null {
  const attr = findAttr(ad, key);
  if (!attr || attr.value.trim() === "") return null;
  return { value: attr.value, label: attr.displayValue };
}

// MULTI_SELECT: the generic endpoint comma-joins both value and displayValue in the same order
// (see AdAttributeService.toResponse), so split and zip them back into pairs.
function multiLabel(ad: AdResponse, key: string): TuitionAttributeValueLabel[] {
  const attr = findAttr(ad, key);
  if (!attr || attr.value.trim() === "") return [];
  const values = attr.value.split(",");
  const labels = attr.displayValue.split(", ");
  return values.map((value, i) => ({ value, label: labels[i] ?? value }));
}

function wrapSingle(label: TuitionAttributeValueLabel | null): TuitionAttributeValueLabel[] {
  return label ? [label] : [];
}

export function buildTuitionDetailFromAd(ad: AdResponse): TuitionClassDetailResponse {
  return {
    id: ad.id,
    slug: ad.slug,
    title: ad.title,
    description: ad.description,
    price: ad.price,
    categorySlug: ad.categorySlug,
    createdAt: ad.createdAt,
    publishedAt: ad.publishedAt,
    academic: {
      subject: textValue(ad, "subject"),
      level: textValue(ad, "grade"),
      curriculum: singleLabel(ad, "curriculum"),
      medium: multiLabel(ad, "medium"),
    },
    classInfo: {
      deliveryModes: wrapSingle(singleLabel(ad, "classMode")),
      classFormats: wrapSingle(singleLabel(ad, "classType")),
      classPurposes: [],
    },
    locations: ad.locations,
    media: ad.media,
    contact: ad.contact ?? { name: ad.seller.displayName, phoneNumber: ad.seller.phone, whatsappNumber: ad.seller.phone },
  };
}

export function buildTuitionCardFromAd(ad: AdResponse): TuitionClassCardResponse {
  const primaryImage = ad.media[0];
  return {
    id: ad.id,
    slug: ad.slug,
    title: ad.title,
    price: ad.price,
    // Raw backend-relative URL, same as the generic MediaResponse.url - callers apply
    // resolveMediaUrl() at render time, same as ClassCard does for ad.media[0].url.
    primaryImageUrl: primaryImage ? primaryImage.url : null,
    primaryLocation: ad.locations[0] ?? null,
    subject: textValue(ad, "subject"),
    level: textValue(ad, "grade"),
    curriculum: singleLabel(ad, "curriculum"),
    medium: multiLabel(ad, "medium"),
  };
}

// Mock-mode stand-in for the real GET /api/tuition/featured (see mockTuitionRepository.ts) -
// unreachable via the app's composed tuitionRepository since getFeaturedTuition always uses the
// real endpoint (same as getClassDetail/getSimilarClasses), kept only to satisfy the
// TuitionRepository interface.
export function buildFeaturedCardFromAd(ad: AdResponse): TuitionFeaturedCardResponse {
  const primaryImage = ad.media[0];
  return {
    id: ad.id,
    slug: ad.slug,
    title: ad.title,
    price: ad.price,
    primaryImageUrl: primaryImage ? primaryImage.url : null,
    primaryLocation: ad.locations[0] ?? null,
    subject: textValue(ad, "subject"),
    level: textValue(ad, "grade"),
    curriculum: singleLabel(ad, "curriculum"),
    medium: multiLabel(ad, "medium"),
    deliveryMode: singleLabel(ad, "classMode"),
    providerName: ad.seller.displayName,
  };
}
