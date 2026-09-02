# CeylonAds Tuition UI — API Call Inventory

Audit of every network call `ceylonads-tuition-ui` can make, classified as:

- **(A) MOCK** — served entirely from frontend-only data, no HTTP request leaves the browser.
- **(B) GENERIC CEYLONADS API** — real HTTP call to a shared backend endpoint also used by the main `ceylonads-ui`.
- **(C) TUITION-ONLY ENDPOINT** — real HTTP call to a dedicated `/api/tuition/*` endpoint.

Last verified: 2026-08-30, against the codebase as of this session.

## Env vars

| Var | Where read | Default | Effect |
|---|---|---|---|
| `VITE_API_BASE_URL` | `src/api/apiClient.ts` | `http://localhost:8080` (`.env.local`/`.env.example`) | Real backend base URL. In dev, Vite proxies `/api` and `/media` to it; in prod builds axios uses it directly as `baseURL`. |
| `VITE_TUITION_DATA_SOURCE` | `src/tuition/api/tuitionApi.ts` | unset → **mock** | Gates only `getDetails`/`getDetailsMap` (decorative schedule/home-visit/teacher data). Set to `"real"` to flip. |
| `VITE_TUITION_PROMOTION_DATA_SOURCE` | `src/tuition/promotion/api/tuitionPromotionApi.ts` | unset → **mock** | Gates only `getHomepagePromotions`/`getDetailPromotions`/`getProfilePromotions`. **Note:** even set to `"real"`, these three throw `"not implemented yet"` — there is no backend for them. Do not flip this in any real environment. |

No favourites or payments feature exists in this frontend's source at all (grep confirms zero call sites) — despite `ceylonads-tuition-ui/CLAUDE.md` describing both as shared capabilities the tuition UI should eventually reuse.

## (A) Mock — no real HTTP request

| Repository | Method(s) | Backing data |
|---|---|---|
| `MockTuitionRepository` (`tuition/api/mockTuitionRepository.ts`) | `getDetails`, `getDetailsMap` | `TUITION_TEMPLATES` in `tuition/data/tuition.mock.ts` — fabricated subject/level/curriculum/schedule/home-visit/teacher-profile data, hash-matched to a real ad's title/attributes. Active by default. |
| `MockTuitionPromotionRepository` (`tuition/promotion/api/mockTuitionPromotionRepository.ts`) | `getHomepagePromotions`, `getDetailPromotions`, `getProfilePromotions` | `TUITION_PROMOTIONS` in `tuition/promotion/data/promotion.mock.ts` — full mock promotion inventory across every placement type, filtered/sorted in-memory via `matching.ts`. Effectively always mock today (the HTTP versions of these three throw). |

Backing model files: `tuition/model/tuition.ts` (types, explicitly documented as "mock fallback — real backend attributes win when present"), `tuition/promotion/model/promotion.ts`.

## (B) Generic CeylonAds API — shared with main ceylonads-ui

Thin wrappers in `src/api/*.ts`, called directly by hooks/pages (not through any tuition repository):

| Endpoint | Wrapper | Called from |
|---|---|---|
| `POST /api/auth/login` | `authApi.ts` → `login` | `auth/AuthContext.tsx` |
| `POST /api/auth/register` | `authApi.ts` → `register` | `auth/AuthContext.tsx` |
| `GET /api/customers/me` | `customerApi.ts` → `getMyProfile` | `pages/PostAd/PostAdWizard.tsx` |
| `GET /api/categories` | `categoryApi.ts` → `listCategories` | `hooks/useCategories.ts` |
| `GET /api/categories/{slug}/attributes` | `categoryApi.ts` → `getCategoryAttributes` | `hooks/useCategoryAttributes.ts` |
| `GET /api/categories/{slug}/filters` | `categoryApi.ts` → `getCategoryFilters` | (available, not currently wired to a page) |
| `GET /api/locations` | `locationApi.ts` → `listLocations` | `hooks/useLocations.ts` (→ `useDistricts`, `useTuitionCategories`) |
| `GET /api/ads` | `adsApi.ts` → `searchAds` | `features/ClassSearch/ClassSearchResults.tsx` |
| `GET /api/ads/{id}` | `adsApi.ts` → `getAd` | (available; not the primary tuition detail path — see `getClassDetail` in bucket C) |
| `GET /api/ads/mine` | `adsApi.ts` → `getMyAds` | `pages/MyAdsPage.tsx`, `pages/EditAdPage.tsx` |
| `GET /api/ads/featured` | `adsApi.ts` → `getFeaturedAds` | (available; homepage uses the tuition-only `getFeaturedTuition` instead) |
| `GET /api/ads/category-featured` | `adsApi.ts` → `getCategoryFeaturedAds` | (available; superseded by tuition-only `getFeaturedTuition`) |
| `POST /api/ads` | `adsApi.ts` → `createAd` | `pages/PostAd/PostAdWizard.tsx` |
| `PUT /api/ads/{id}` | `adsApi.ts` → `updateAd` | `pages/PostAd/PostAdWizard.tsx` |
| `DELETE /api/ads/{id}` | `adsApi.ts` → `deactivateAd` | `pages/MyAdsPage.tsx` |
| `POST /api/ads/{adId}/media` | `mediaApi.ts` → `uploadAdMedia` | `pages/PostAd/PostAdWizard.tsx` |
| `GET /api/promotion-plans` | `promotionApi.ts` → `listActivePromotionPlans` | (available; not currently wired to a page — no promotion-purchase UI exists in the tuition UI yet) |

Plus raw image loads (not JSON `apiClient` calls, but real HTTP against the shared media host): `resolveMediaUrl()` in `apiClient.ts`, used by every ad-card/gallery component to build `<img src>` URLs.

`MockTuitionRepository` internally calls `getAd`, `searchAds`, and `getCategoryFeaturedAds` (bucket B) to synthesize its mock cards from real ad data — but this path is unreachable in practice, since `getClassDetail`/`getSimilarClasses`/`getFeaturedTuition`/`getLatestClasses` always route through the real tuition-only endpoints instead (see bucket C), regardless of the mock/real env toggle.

## (C) Tuition-only endpoints

All via `HttpTuitionRepository` (`tuition/api/tuitionApi.ts`) and `HttpTuitionPromotionRepository` (`tuition/promotion/api/tuitionPromotionApi.ts`):

| Endpoint | Method | Gating | Consumed by |
|---|---|---|---|
| `GET /api/tuition/ads/{id}/details` | `getDetails` | Only if `VITE_TUITION_DATA_SOURCE=real` (default: mock) | `hooks/useTuitionDetails.ts` |
| `GET /api/tuition/classes/{slug}` | `getClassDetail` | Always HTTP | `pages/ClassDetailPage.tsx` |
| `GET /api/tuition/classes/{slug}/similar` | `getSimilarClasses` | Always HTTP | `pages/ClassDetailPage.tsx` ("Similar Classes") |
| `GET /api/tuition/featured` | `getFeaturedTuition` | Always HTTP | `hooks/useFeaturedTuition.ts` (homepage carousel) |
| `GET /api/tuition/classes` | `getLatestClasses` | Always HTTP | `hooks/useLatestTuitionClasses.ts` (homepage "Latest Classes") |
| `GET /api/tuition/filters` | `getFilters` | Always HTTP | `hooks/useTuitionFilters.ts` (search filter bar master data) |
| `GET /api/tuition/promotions` | `getSearchPromotions` | Always HTTP | `hooks/useTuitionPromotions.ts` (search page top banner + sidebar) |

## Summary

- **24** direct `apiClient` call sites total: 17 generic (bucket B), 7 tuition-only (bucket C).
- **Mock today:** decorative per-ad schedule/teacher enrichment, and homepage/detail/profile promotion placements (the latter have no backend to fall back to even if the env var is flipped).
- **Real and tuition-only:** class detail, similar classes, featured carousel, latest classes, filter master data, and search-page promotions.
- **Real and generic:** auth, profile, categories, locations, ad CRUD/search/media upload — reused as-is from the main CeylonAds platform, exactly per `ceylonads-tuition-ui/CLAUDE.md`'s "one shared backend" architecture.
- **Gaps vs CLAUDE.md's stated intent:** favourites and promotion-purchase flows are described as reusable but have no UI/call sites yet; homepage/detail/profile promotions remain mock-only pending a backend beyond the search page's.
