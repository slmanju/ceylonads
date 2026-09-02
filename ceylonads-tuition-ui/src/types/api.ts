// Types mirror the shared CeylonAds OpenAPI schema (GET /v3/api-docs). Only the shapes the
// tuition UI actually consumes are declared here — see ceylonads-ui/src/types/api.ts for the
// full backend contract.

export type Role = "CUSTOMER" | "MODERATOR" | "ADMIN";

export type AdStatus =
  | "DRAFT"
  | "PENDING_REVIEW"
  | "ACTIVE"
  | "REJECTED"
  | "SOLD"
  | "EXPIRED"
  | "DEACTIVATED";

export type LocationType = "PROVINCE" | "DISTRICT" | "CITY";

export type SortOption = "newest" | "oldest" | "price_asc" | "price_desc";

export interface MediaResponse {
  id: number;
  url: string;
  contentType: string;
  displayOrder: number;
}

export type AttributeDataType = "TEXT" | "NUMBER" | "DECIMAL" | "BOOLEAN" | "SELECT" | "MULTI_SELECT";

export interface AttributeOptionResponse {
  id: number;
  value: string;
  label: string;
  displayOrder: number;
  active: boolean;
}

export interface AttributeDefinitionResponse {
  id: number;
  categoryId: number;
  key: string;
  name: string;
  dataType: AttributeDataType;
  required: boolean;
  filterable: boolean;
  unit: string | null;
  displayOrder: number;
  active: boolean;
  options: AttributeOptionResponse[];
}

export interface AdAttributeResponse {
  key: string;
  name: string;
  dataType: AttributeDataType;
  value: string;
  displayValue: string;
  unit: string | null;
}

export interface AdSellerResponse {
  id: number;
  displayName: string;
  phone: string | null;
}

export interface AdContactResponse {
  name: string;
  phoneNumber: string | null;
  whatsappNumber: string | null;
}

export interface AdContactOverrideResponse {
  contactName: string | null;
  phoneNumber: string | null;
  whatsappNumber: string | null;
}

export interface AdResponse {
  id: number;
  slug: string;
  title: string;
  description: string;
  price: number;
  category: string;
  categorySlug: string;
  locations: LocationResponse[];
  seller: AdSellerResponse;
  status: AdStatus;
  createdAt: string;
  publishedAt: string | null;
  reviewedAt: string | null;
  media: MediaResponse[];
  promoted: boolean;
  attributes: AdAttributeResponse[];
  contact: AdContactResponse | null;
  contactOverride: AdContactOverrideResponse | null;
}

export interface CreateAdRequest {
  title: string;
  description: string;
  price: number;
  categorySlug: string;
  locationSlugs: string[];
  attributes?: Record<string, string>;
  contactName?: string;
  phoneNumber?: string;
  whatsappNumber?: string;
}

// Payload for POST/PUT /api/tuition/classes - the tuition-scoped equivalent of CreateAdRequest.
// Always creates/keeps a TUITION listing server-side (see ceylonads-api's TuitionClassService);
// using the generic CreateAdRequest against /api/ads instead would silently tag the ad MAIN_SITE.
export interface TuitionClassCreateRequest {
  title: string;
  description: string;
  price: number;
  categorySlug: string;
  locationSlugs: string[];
  contactName?: string;
  phoneNumber?: string;
  whatsappNumber?: string;
  subject?: string;
  level?: string;
  curriculum?: string;
  medium?: string[];
  deliveryMode?: string;
  classFormat?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface CategoryResponse {
  id: number;
  name: string;
  slug: string;
  parentId: number | null;
  displayOrder: number;
  active: boolean;
}

export interface LocationResponse {
  id: number;
  name: string;
  slug: string;
  type: LocationType;
  parentId: number | null;
}

export interface CategoryFiltersResponse {
  category: CategoryResponse;
  filters: AttributeDefinitionResponse[];
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  displayName: string;
  phone?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  username: string;
  role: Role;
}

export interface CustomerResponse {
  id: number;
  username: string;
  email: string;
  displayName: string;
  phone: string | null;
  status: string;
}

export interface AdSearchParams {
  q?: string;
  category?: string;
  location?: string;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
  sort?: SortOption;
  // Keys already carry the "attr." prefix the backend expects, e.g. "attr.classMode".
  attributeFilters?: Record<string, string>;
}

export interface ApiErrorBody {
  message?: string;
  errors?: Record<string, string>;
  [key: string]: unknown;
}

// GET /api/tuition/classes/{slug} and .../similar - the dedicated, lean tuition read API (see
// ceylonads-api's `tuition` package). Distinct from AdResponse: no seller/status/promoted/raw
// attributes, and SELECT/MULTI_SELECT attribute values already come back as resolved value+label
// pairs instead of the generic comma-joined AdAttributeResponse shape.
export interface TuitionAttributeValueLabel {
  value: string;
  label: string;
}

export interface TuitionAcademicInfo {
  subject: string | null;
  level: string | null;
  curriculum: TuitionAttributeValueLabel | null;
  medium: TuitionAttributeValueLabel[];
}

export interface TuitionClassInfoResponse {
  deliveryModes: TuitionAttributeValueLabel[];
  classFormats: TuitionAttributeValueLabel[];
  classPurposes: TuitionAttributeValueLabel[];
}

export interface TuitionClassDetailResponse {
  id: number;
  slug: string;
  title: string;
  description: string;
  price: number;
  categorySlug: string;
  createdAt: string;
  publishedAt: string | null;
  academic: TuitionAcademicInfo;
  classInfo: TuitionClassInfoResponse;
  locations: LocationResponse[];
  media: MediaResponse[];
  contact: AdContactResponse;
}

export interface TuitionClassCardResponse {
  id: number;
  slug: string;
  title: string;
  price: number;
  primaryImageUrl: string | null;
  primaryLocation: LocationResponse | null;
  subject: string | null;
  level: string | null;
  curriculum: TuitionAttributeValueLabel | null;
  medium: TuitionAttributeValueLabel[];
}

// GET /api/tuition/featured - the isolated Featured Tuition homepage carousel card shape (see
// ceylonads-api's TuitionFeaturedCardResponse). Every card returned by that endpoint is, by
// construction, an active featured placement - there is no separate "featured" flag on the DTO.
export interface TuitionFeaturedCardResponse {
  id: number;
  slug: string;
  title: string;
  price: number | null;
  primaryImageUrl: string | null;
  primaryLocation: LocationResponse | null;
  subject: string | null;
  level: string | null;
  curriculum: TuitionAttributeValueLabel | null;
  medium: TuitionAttributeValueLabel[];
  deliveryMode: TuitionAttributeValueLabel | null;
  providerName: string | null;
}

// GET /api/tuition/filters - dedicated master-data endpoint for the Tuition search UI's filter
// panel (see ceylonads-api's TuitionFilterMetadataResponse), spanning the whole tuition vertical
// (education-tuition and every direct child category), not a single leaf category. Isolated from
// /api/categories/{slug}/filters.
export interface TuitionFilterMetadataResponse {
  subjects: TuitionAttributeValueLabel[];
  levels: TuitionAttributeValueLabel[];
  curricula: TuitionAttributeValueLabel[];
  mediums: TuitionAttributeValueLabel[];
  deliveryModes: TuitionAttributeValueLabel[];
}

// GET /api/tuition/promotions - Tuition search page's top banner + 3 sidebar placements (see
// ceylonads-api's TuitionPromotionResponse/TuitionPromotionsResponse). type is "AD" (an ad-backed
// sidebar card - targetId/adSlug identify the ad) or "BANNER" (a plain image banner - targetUrl is
// the click-through link). Grouped by slot so all 4 placements come back in one request.
export interface TuitionPromotionResponse {
  id: number;
  slot: string;
  type: "AD" | "BANNER";
  title: string | null;
  subtitle: string | null;
  imageUrl: string | null;
  badge: string;
  ctaLabel: string | null;
  targetUrl: string | null;
  targetType: "AD" | "EXTERNAL";
  targetId: number | null;
  adSlug: string | null;
  displayOrder: number;
}

export interface TuitionPromotionsResponse {
  topBanner: TuitionPromotionResponse[];
  sidebarTop: TuitionPromotionResponse[];
  sidebarMiddle: TuitionPromotionResponse[];
  sidebarBottom: TuitionPromotionResponse[];
}

export interface PromotionPlanResponse {
  id: number;
  code: string;
  name: string;
  description: string;
  placementType: string;
  slotId: number;
  slotCode: string;
  slotName: string;
  categoryId: number | null;
  categorySlug: string | null;
  categoryName: string | null;
  slotCapacity: number;
  slotVisibleCount: number;
  durationDays: number;
  // price is always the plan's permanent base price. currentPrice is what a customer pays right
  // now - equal to price unless a promotion campaign is active, in which case discounted/
  // discountAmount/discountPercent/campaignName/campaignEndsAt describe the difference. Discount
  // math is always resolved server-side (PromotionPricingService), never recomputed here.
  price: number;
  currentPrice: number;
  discounted: boolean;
  discountAmount: number;
  discountPercent: number | null;
  campaignName: string | null;
  campaignEndsAt: string | null;
  active: boolean;
  paymentRequired: boolean;
  approvalRequired: boolean;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
}

// A plan one of my classes is eligible to buy, paired with the slot's live availability - see
// PromotionService#compatiblePlansForTuitionAd on the backend.
export interface CompatiblePromotionPlanResponse {
  plan: PromotionPlanResponse;
  available: boolean;
  remainingCapacity: number;
}

export type PromotionStatus = "PENDING_PAYMENT" | "PENDING_APPROVAL" | "ACTIVE" | "EXPIRED" | "CANCELLED";

export interface PromotionResponse {
  id: number;
  adId: number | null;
  adTitle: string | null;
  promotionPlanId: number;
  promotionPlanCode: string;
  promotionPlanName: string;
  slotId: number;
  slotCode: string;
  price: number;
  durationDays: number;
  paymentRequired: boolean;
  status: PromotionStatus;
  createdAt: string;
  startsAt: string | null;
  endsAt: string | null;
}

// GET /api/tuition/promotions/campaign - storefront banner/modal presentation only, never
// authoritative for pricing (see ceylonads-api's TuitionCampaignResponse).
export interface TuitionCampaignResponse {
  code: string;
  name: string;
  headline: string;
  message: string;
  ctaLabel: string;
  startsAt: string;
  endsAt: string;
  showBanner: boolean;
  showModal: boolean;
}
