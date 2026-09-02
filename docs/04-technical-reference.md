# CeylonAds — Technical Reference

**Purpose:** Fast “what exists right now?” reference  
**Document status:** Living document  
**Last updated:** 2026-08-26

## 1. Current Product Shape

CeylonAds is a Sri Lanka-focused classifieds marketplace with:
- General marketplace UI.
- Spring Boot API.
- PostgreSQL database.
- GCS-based media support.
- Customer authentication.
- Admin and moderator roles.
- Advertisement moderation.
- Search/filtering.
- Promotion plans/placements.
- Flyway-managed schema/master data.
- Vertical-site strategy for tuition and boarding.

## 2. Repository / Stack

### Backend
```text
Java 26
Spring Boot 4.x
Gradle
Spring Security
JWT
Spring Data JPA
Flyway
PostgreSQL
Swagger/OpenAPI
```

### Frontend
```text
React
Vite
TypeScript
React Router
Axios
```

### Cloud / Infrastructure
```text
Google Cloud Run
Google Cloud Storage
PostgreSQL
```

Earlier persistence experiments included Supabase PostgreSQL.

## 3. Backend Domains

Known package-by-domain structure includes:

```text
auth
customer
admin
ad
category
location
media
search
promotion
payment
common
```

## 4. Roles

```text
CUSTOMER
MODERATOR
ADMIN
```

### Customer
- Manage own ads.
- Use customer marketplace functionality.
- Request promotions.

### Moderator
- Review ads.
- Approve/reject ads.
- Must not approve promotions.

### Admin
- Review ads.
- Approve/reject ads.
- Approve promotion-related operations.
- Perform administrative actions.

## 5. Authentication

Implemented design:
- Username/password login.
- JWT-based API authentication.
- Role-based endpoint authorization.

Requested/current account feature:
- Logged-in user password reset/change.
- No public forgot-password flow at this stage.

## 6. Advertisement Features

Known implemented/designed functionality:
- Create ads.
- Public ad listing/detail.
- Category association.
- Location support.
- Media.
- Description.
- Price.
- Moderation lifecycle.
- Promotion relationship.
- Shareable public routes/slugs.

UI behavior discussed:
- Long descriptions with show-more.
- No meaningless empty placeholders.
- Mobile-friendly ad details.
- Category-aware navigation/filtering.

## 7. Categories

The category master data was restructured to a hierarchy.

Important design rule:
- Do not expose every hierarchy level in one flat category dropdown.

Known category work includes domains such as:
- Vehicles.
- Property.
- Mobile.
- Tuition.
- Services.
- Food & Beverages.
- Other marketplace categories.

Flyway category/attribute master-data migration:
```text
V3
```

Known prepared category dataset:
```text
~89 category records
```

## 8. Locations

Structured Sri Lankan location master data:

```text
9 provinces
26 districts
258 cities
293 total rows
```

Flyway migration:
```text
V2
```

Location is optional for online/non-geographic services.

## 9. Search and Filters

Implemented/worked-on areas:
- Backend ad search.
- Frontend filters.
- Category filter.
- Location filter.
- Search page category context.

Known issue discovered during development:
- Broad text matching could return semantically unrelated results, e.g. a query such as `tea` matching tuition-related content.

Direction:
- Search predicates must be more intentional and category-aware.
- Category navigation and normal search should not behave as two conflicting modes.

## 10. Promotions

Promotion system concepts:
- Promotion slots.
- Promotion plans.
- Promotions.
- Promotion status.
- Start/end dates.
- Capacity.

Known placement:
```text
AD_DETAIL_SIDEBAR
```

Other placements have been designed for:
- Homepage.
- Category areas.
- Tuition.
- Boarding.

Known backend method pattern:
```text
resolve category featured slot
→ query active promotions
→ respect slot capacity
→ map promoted ads
```

Performance note:
- Promotion-to-ad mapping was identified as a possible N+1 source and should use deliberate fetching.

## 11. Payments

Promotion payments must support online and offline/manual scenarios.

Known valid scenario:
```text
CASH
```

A legacy database CHECK constraint previously conflicted with this method.

Design rule:
- Payment slip/evidence is optional when the payment method does not require one.
- Admin should be able to approve valid manual-payment promotion requests.

## 12. Media

Current architectural direction:

```text
Database
    stores media key
        ↓
Media service / URL resolver
        ↓
GCS/public URL
```

Legacy full-URL persistence has been removed from the clean schema design.

Known cloud bucket history included development bucket naming such as:
```text
ceylonads-dev
```

Bucket names and credentials remain environment configuration, not domain data.

## 13. Flyway

Flyway was introduced for controlled schema evolution.

Known dependencies included:
```text
spring-boot-flyway
flyway-core
flyway-database-postgresql
```

Known migration sequence:

```text
V1  baseline schema
V2  Sri Lanka location master data
V3  category + attribute master data
```

Baseline decisions:
- Do not include old `media.url`.
- Do not include obsolete `ads.location_id` if superseded by the current model.
- Do not include stale payment method checks.
- Do not put legacy cleanup/drop statements into a clean initial baseline.

## 14. Sample Data Strategy

CeylonAds development evolved from large Java seeders toward better-separated master/sample data.

Preferred rule:
- Flyway: schema + true master/reference data.
- Development/test seeding: sample transactional records.
- Production: no dependency on demo seeders.

Past seed domains included:
- People/accounts.
- Categories.
- Cars.
- Motorcycles.
- Property.
- Mobile.
- Tuition.
- Services.
- Promotion plans.
- Promotions.

A previous large single `@Transactional` seeding flow was identified as undesirable because one failure could roll back unrelated seed sections.

## 15. Known Performance Work

### N+1
N+1 behavior was found/considered in paths involving:
- `adMapper.toResponse(...)`.
- Promotion queries.
- Associated entity resolution.

Expected fixes:
- Fetch joins/entity graphs/projections.
- Verify SQL rather than assuming mapper calls are inexpensive.

### Frontend caching
Stable data suitable for caching includes:
- Categories.
- Locations.
- Potentially promotion/reference configuration.

The React application should not refetch stable reference data on every navigation without reason.

## 16. SPA Routing

Known deployment issue:
- Direct navigation to frontend routes could produce 404/unauthorized behavior.

Required behavior:
- Non-API frontend routes fall back to `index.html`.
- `/api/**` remains API-controlled.
- Security permits intended public frontend routes.
- Public ad links remain directly shareable.

## 17. Main Public Route Direction

Public ads should use stable shareable URLs, ideally slugs.

Example:

```text
/ads/nokia-1100-123
```

Slug parsing/public lookup code has used the pattern of extracting a trailing numeric ID.

Where slug routing is used, invalid slugs should return a proper not-found response rather than accidental authorization errors.

## 18. Vertical Sites

### Tuition UI

Phased strategy:
1. Build tuition-focused UI using existing APIs without changing backend behavior unnecessarily.
2. Add tuition-specific backend/domain features.
3. Surface tuition-specific capabilities in main CeylonAds.
4. Add tuition-specific promotion slots.

Important tuition features:
- Online classes.
- Multiple locations.
- Sinhala/English.
- Group classes.
- Host/teacher visit.

### Boarding UI

Phased strategy:
1. Build boarding UI using existing CeylonAds/backend capabilities.
2. Add boarding-specific features without breaking existing behavior.
3. Introduce boarding promotion placements.
4. Surface richer boarding/property data in the main marketplace UI.

## 19. Deployment Notes

CeylonAds has been deployed using Google Cloud Run.

Questions/decisions previously addressed include:
- Custom domains.
- Cloud Run cold starts/minimum instances.
- PostgreSQL provider choice.
- GCS media permissions.

Architecture should remain portable enough for future infrastructure changes.

## 20. Known Issues / Watch List

- Search relevance needs continued testing.
- Verify all list/detail queries for N+1 problems.
- Keep production database migrations synchronized with Flyway.
- Avoid flattening category trees in UI controls.
- Avoid exposing all cities in an unusable giant selector.
- Ensure media bucket public/read strategy matches application URL behavior.
- Keep SPA fallback rules separate from API security.
- Keep MODERATOR and ADMIN permissions distinct.
- Keep vertical-specific logic from polluting generic marketplace code.
- Verify all enum/database CHECK values remain synchronized.

## 21. Documentation Rule for Future Vibe-Coding Phases

Every significant implementation prompt should end with:

> Update the relevant files under `/docs` to reflect the implementation. Document only functionality that actually exists. Clearly mark planned or partially implemented behavior. Update architecture, database, and technical reference documentation whenever the implementation changes those areas.

## 22. Suggested `/docs` Layout

```text
docs/
├── 01-business-requirements.md
├── 02-architecture.md
├── 03-database.md
└── 04-technical-reference.md
```

`CLAUDE.md` should link to these files but should not duplicate them.
