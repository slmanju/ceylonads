// Types generated from the CeylonAds OpenAPI schema (GET /v3/api-docs).

export type Role = "CUSTOMER" | "MODERATOR" | "ADMIN";

export type AdStatus =
  | "DRAFT"
  | "PENDING_REVIEW"
  | "ACTIVE"
  | "REJECTED"
  | "SOLD"
  | "EXPIRED"
  | "DEACTIVATED";

export type CustomerStatus = "ACTIVE" | "SUSPENDED" | "DISABLED";

export type LocationType = "PROVINCE" | "DISTRICT" | "CITY";

export type PlacementType = "HOME_FEATURED" | "HOME_BANNER" | "CATEGORY_FEATURED" | "CATEGORY_BANNER" | "TOP_SEARCH";

export type PromotionStatus = "PENDING_PAYMENT" | "PENDING_APPROVAL" | "ACTIVE" | "EXPIRED" | "CANCELLED";

export type PromotionKind = "AD_PROMOTION" | "BANNER_PROMOTION";

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

// Resolved/effective contact for buyers: ad-specific override where set, otherwise the seller's
// account contact. Only populated on the ad detail response.
export interface AdContactResponse {
  name: string;
  phoneNumber: string | null;
  whatsappNumber: string | null;
}

// Raw ad-specific contact override as stored, each field independently nullable when unset. Only
// populated on responses returned to the ad's own owner (create/update/mine) so Edit can tell "no
// override" apart from "override happens to match the account value".
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
  // 0..N: e.g. empty for online tuition/remote services, one for a property, several for a
  // teacher/service covering multiple towns.
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
  // 0..N location slugs; whether zero/one/many is allowed is category-dependent and enforced
  // by the backend (e.g. online tuition requires none, a property requires one).
  locationSlugs: string[];
  attributes?: Record<string, string>;
  // Ad-specific contact override, all optional: a blank/omitted value falls back to the seller's
  // account contact.
  contactName?: string;
  phoneNumber?: string;
  whatsappNumber?: string;
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

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ChangePasswordResponse {
  message: string;
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
  status: CustomerStatus;
}

export interface AdminCategoryRequest {
  name: string;
  slug: string;
  parentSlug?: string;
  displayOrder?: number;
}

export interface AdminAttributeOptionRequest {
  value: string;
  label: string;
  displayOrder: number;
}

export interface AdminAttributeOptionUpdateRequest {
  label: string;
  displayOrder: number;
  active: boolean;
}

export interface AdminAttributeDefinitionRequest {
  key: string;
  name: string;
  dataType: AttributeDataType;
  required: boolean;
  filterable: boolean;
  searchable: boolean;
  unit?: string;
  displayOrder: number;
  options?: AdminAttributeOptionRequest[];
}

export interface AdminAttributeDefinitionUpdateRequest {
  name: string;
  required: boolean;
  filterable: boolean;
  searchable: boolean;
  unit?: string;
  displayOrder: number;
  active: boolean;
}

export interface AdminLocationRequest {
  name: string;
  slug: string;
  type: LocationType;
  parentSlug?: string;
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
  // Keys already carry the "attr." prefix the backend expects, e.g. "attr.make" or "attr.year.min".
  attributeFilters?: Record<string, string>;
}

export interface ApiErrorBody {
  message?: string;
  errors?: Record<string, string>;
  [key: string]: unknown;
}

export interface PromotionPlanResponse {
  id: number;
  code: string;
  name: string;
  description: string;
  placementType: PlacementType;
  slotId: number;
  slotCode: string;
  slotName: string;
  categoryId: number | null;
  categorySlug: string | null;
  categoryName: string | null;
  slotCapacity: number;
  slotVisibleCount: number;
  durationDays: number;
  price: number;
  active: boolean;
  paymentRequired: boolean;
  approvalRequired: boolean;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface CompatiblePromotionPlanResponse {
  plan: PromotionPlanResponse;
  available: boolean;
  remainingCapacity: number;
}

export interface PromotionResponse {
  id: number;
  kind: PromotionKind;
  adId: number | null;
  adTitle: string | null;
  customerId: number;
  customerDisplayName: string;
  promotionPlanId: number;
  promotionPlanCode: string;
  promotionPlanName: string;
  slotId: number;
  slotCode: string;
  placementType: PlacementType;
  bannerMediaUrl: string | null;
  targetUrl: string | null;
  price: number;
  durationDays: number;
  paymentRequired: boolean;
  paymentWaived: boolean;
  status: PromotionStatus;
  createdAt: string;
  startsAt: string | null;
  endsAt: string | null;
}

export interface CreatePromotionRequest {
  adId: number;
  promotionPlanId: number;
}

export interface AdminPromotionPlanRequest {
  code: string;
  name: string;
  description: string;
  slotId: number;
  durationDays: number;
  price: number;
  paymentRequired?: boolean;
  approvalRequired?: boolean;
  displayOrder?: number;
}

export interface AdminPromotionPlanUpdateRequest {
  name: string;
  description: string;
  price: number;
  durationDays: number;
  active: boolean;
  paymentRequired?: boolean;
  approvalRequired?: boolean;
  displayOrder?: number;
}

// Which storefront/vertical a slot's inventory belongs to (see ads.source_channel /
// promotion_slots.source_channel on the backend).
export type SourceChannel = "MAIN_SITE" | "TUITION" | "BOARDING";

export interface PromotionSlotResponse {
  id: number;
  code: string;
  name: string;
  description: string;
  placementType: PlacementType;
  categoryId: number | null;
  categorySlug: string | null;
  categoryName: string | null;
  sourceChannel: SourceChannel;
  capacity: number;
  visibleCount: number;
  displayOrder: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PromotionSlotAdminRequest {
  code: string;
  name: string;
  description: string;
  placementType: PlacementType;
  categorySlug?: string;
  sourceChannel: SourceChannel;
  capacity: number;
  visibleCount?: number;
  displayOrder?: number;
}

export interface PromotionSlotUpdateRequest {
  name: string;
  description: string;
  capacity: number;
  visibleCount?: number;
  displayOrder?: number;
  active: boolean;
}

export interface PromotionSlotAvailabilityResponse {
  slotId: number;
  available: boolean;
  capacity: number;
  remainingCapacity: number;
  requestedStart: string;
  requestedEnd: string;
}

export interface PromotionSlotUsageResponse {
  slot: PromotionSlotResponse;
  activeCount: number;
  pendingPaymentCount: number;
  remainingCapacity: number;
  activePromotions: PromotionResponse[];
  pendingPromotions: PromotionResponse[];
}

export interface PromotionBannerResponse {
  promotionId: number;
  bannerMediaUrl: string;
  targetUrl: string | null;
  startsAt: string | null;
  endsAt: string | null;
}

export interface AdminCreatePromotionRequest {
  customerId: number;
  promotionPlanId: number;
  adId?: number;
  bannerMediaId?: number;
  targetUrl?: string;
  paymentWaived: boolean;
}

export type PaymentMethod = "BANK_TRANSFER" | "CASH" | "OTHER";

export type PaymentStatus = "PENDING" | "SUBMITTED" | "APPROVED" | "REJECTED" | "CANCELLED";

export interface PaymentResponse {
  id: number;
  paymentReference: string;
  promotionId: number;
  adId: number;
  adTitle: string;
  promotionPlanName: string;
  placementType: PlacementType;
  customerId: number;
  customerDisplayName: string;
  customerPhone: string | null;
  customerEmail: string;
  amount: number;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  bankReference: string | null;
  receiptUrl: string | null;
  customerNote: string | null;
  adminNote: string | null;
  submittedAt: string | null;
  reviewedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentSummaryResponse {
  id: number;
  paymentReference: string;
  promotionId: number;
  adId: number;
  adTitle: string;
  promotionPlanName: string;
  customerId: number;
  customerDisplayName: string;
  amount: number;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  bankReference: string | null;
  submittedAt: string | null;
  createdAt: string;
}

export interface SubmitPaymentRequest {
  bankReference: string;
  customerNote?: string;
}

export interface RejectPaymentRequest {
  reason: string;
}

export interface VerifyPaymentRequest {
  paymentMethod?: PaymentMethod;
  adminNote?: string;
}

export interface BankTransferDetailsResponse {
  bankName: string;
  accountName: string;
  accountNumber: string;
  branch: string;
  instructions: string;
}

export interface PaymentCountResponse {
  count: number;
}
