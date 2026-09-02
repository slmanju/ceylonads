# CeylonAds — Business Requirements Document

**Document status:** Living document  
**Product:** CeylonAds  
**Scope:** Current marketplace platform and planned vertical extensions  
**Last updated:** 2026-08-26

## 1. Purpose

CeylonAds is a Sri Lanka-focused classifieds marketplace where individuals and businesses can publish, discover, search, and promote advertisements across multiple categories.

The platform is intended to support a broad general marketplace while also allowing focused vertical experiences such as tuition, boarding/property, vehicles, and other domain-specific sites that share the same backend and data.

## 2. Product Goals

- Provide a simple Sri Lanka-friendly classifieds experience.
- Make ad discovery fast through structured categories, location filters, search, and domain-specific attributes.
- Allow customers to publish and manage their own ads.
- Support moderation before public publication where required.
- Monetize through promotional placements rather than charging for every basic listing.
- Allow promotions to work with online and offline/manual payment methods.
- Reuse the same marketplace backend for specialized websites such as tuition and boarding.
- Keep the platform extensible without forcing unrelated domains into one rigid UI.

## 3. Primary Actors

### 3.1 Visitor
A non-authenticated user who can:
- Browse public ads.
- Search ads.
- Filter by category and location.
- Open ad detail pages.
- View promoted/featured listings.
- Navigate to domain-specific vertical experiences.

### 3.2 Customer
An authenticated marketplace user who can:
- Create ads.
- Edit eligible ads.
- Manage their ads.
- Upload ad media.
- Request promotional placement.
- Submit optional payment proof where applicable.
- Change their own password while logged in.

### 3.3 Moderator
A privileged user who can:
- Review advertisements.
- Approve or reject advertisements.
- Create ads where permitted.
- Perform moderation tasks.
- Not approve promotions/payment-sensitive operations reserved for administrators.

### 3.4 Administrator
A privileged user with platform-management capabilities, including:
- Ad moderation.
- Promotion approval.
- Promotion plan and slot administration where exposed.
- Account/administrative operations.
- Review of offline/manual payment evidence.
- Platform oversight.

## 4. Marketplace Capabilities

### 4.1 Advertisement Creation

Users must be able to create an advertisement with the information required by its category.

Common ad data includes:
- Title.
- Description.
- Category.
- Location where relevant.
- Price where relevant.
- Contact information.
- Media/images.
- Category-specific attributes.

Not every category must require the same fields.

### 4.2 Category Taxonomy

Categories must support hierarchy.

Example:

```text
Vehicles
├── Cars
├── Motorcycles
├── Three Wheelers
├── Vans
├── Buses
└── Lorries & Trucks
```

The UI should primarily present meaningful top-level categories and progressively expose lower-level categories.

The taxonomy must support domain-specific attributes and future extensions without requiring a redesign of the entire marketplace.

### 4.3 Location

Location selection must be optional for advertisements that are not tied to a physical place.

The master location model supports:
- Provinces.
- Districts.
- Cities.

Current master data contains a structured Sri Lankan location hierarchy.

Online-only services, such as some tuition offerings, may operate without a physical location.

### 4.4 Search

Users must be able to search public advertisements using free text and structured filters.

Search should:
- Avoid returning unrelated categories merely because a substring appears in another field.
- Respect selected category context.
- Respect optional location filters.
- Support additional domain-specific filters over time.
- Return public/eligible ads only.

### 4.5 Advertisement Detail

The detail page should provide:
- Advertisement title.
- Price where applicable.
- Full description.
- Media gallery.
- Location where applicable.
- Seller/contact information.
- Relevant category attributes.
- Promotion placements configured for the detail experience.

Long descriptions may be collapsed in the UI with an explicit “show more” experience.

### 4.6 Advertisement Lifecycle

The marketplace supports moderation-oriented lifecycle states.

Typical lifecycle:

```text
Draft/Create
    ↓
Pending Review
    ↓
Approved / Published
    ↓
Expired / Closed / Removed
```

Rejected or otherwise invalid ads must not become publicly discoverable.

Exact enum values should be treated as implementation-level details and documented in the database/technical reference.

## 5. Promotion and Monetization

### 5.1 Promotion Model

CeylonAds monetizes through promotion placements.

The design separates:
- Promotion slots/placements.
- Promotion plans.
- Promotion requests/records.
- Payments/payment evidence.

Examples of promotion placements include:
- Homepage featured ads.
- Category featured ads.
- Ad-detail sidebar or related detail placement.
- Vertical-site promotional areas.

### 5.2 Promotion Capacity

A promotion slot may define a capacity.

The platform must ensure that UI retrieval respects the slot capacity and the requested limit.

### 5.3 Promotion Approval

A customer may request a promotion for an advertisement.

Promotion activation may require administrative review.

Moderators must not gain administrator-only promotion approval permissions.

### 5.4 Payment

The product must support real-world Sri Lankan payment scenarios, including payments that do not originate from an online card transaction.

Supported business scenarios include:
- Online gateway payments when introduced.
- Bank transfer/manual proof.
- Cash/manual hand payment.
- Other explicitly supported payment methods.

Payment proof/slip must not be mandatory for payment methods where no slip exists.

Legacy database constraints must not prevent valid application payment methods.

## 6. Media

Advertisements may have multiple images/media items.

Business rules:
- Database records store media keys/object identifiers rather than environment-specific public URLs.
- Public URLs are resolved by the application/storage layer.
- Storage may differ by environment.
- Media must remain associated with its owning advertisement.
- Broken/empty placeholder images should not be shown in the UI.

## 7. Authentication and Account Requirements

Current account model includes customer and privileged roles.

Requirements include:
- Username/password authentication.
- JWT-based authenticated API access.
- Role-based authorization.
- Logged-in password change/reset capability.
- No public “forgot password” flow is required at the current stage.

The user-facing menu should prefer meaningful identity information over confusing technical usernames where possible.

## 8. Vertical Marketplace Extensions

CeylonAds is designed so separate focused websites can share the same backend.

### 8.1 Shared principles

Each vertical site should:
- Reuse existing backend capabilities where possible.
- Share listings with the main CeylonAds marketplace.
- Avoid duplicating the same underlying ad data.
- Support its own UI, search experience, and promotion placements.
- Preserve common authentication where practical.

### 8.2 Tuition

Tuition requirements include:
- Individual and group classes.
- Host/teacher visit options.
- Online classes.
- Multiple locations.
- Multiple languages such as Sinhala and English.
- Category/domain-specific presentation.
- Tuition-specific promotional placements.

### 8.3 Boarding / Property

Boarding extension requirements include:
- Reuse of existing CeylonAds property/boarding data.
- Dedicated boarding-focused UI.
- Domain-specific features added without breaking the main marketplace.
- Boarding-specific promotion placements.
- Selected boarding/property data surfaced back into the main CeylonAds UI.

## 9. SEO and Shareability

Public advertisements should have stable, shareable URLs.

Slug-based routes are preferred where practical.

Example:

```text
/ads/nokia-1100-123
```

Direct navigation to public frontend routes must work in deployed SPA hosting and must not be blocked by backend security configuration.

## 10. Master and Reference Data

The platform has master/reference data for areas such as:
- Locations.
- Categories.
- Category attributes.
- Promotion slots.
- Promotion plans.
- Administrative seed accounts for non-production/sample environments where appropriate.

Master data should be managed independently from sample transactional data.

Flyway should own schema/reference migrations that must remain consistent across environments.

## 11. Sample and Development Data

Development/test environments may contain realistic sample:
- Accounts.
- Ads.
- Promotions.
- Media references.

Sample data should be deterministic enough to support repeatable UI and integration testing.

Production must not depend on development sample-data loaders.

## 12. Non-Functional Business Requirements

### Performance
- Avoid obvious N+1 query patterns.
- Avoid refetching stable frontend reference data unnecessarily.
- Search and detail pages should remain responsive as ad count grows.

### Security
- Public endpoints must expose only intended public data.
- Privileged operations must enforce role checks.
- Private/admin APIs must not become accessible because of SPA routing rules.

### Reliability
- Schema evolution must use controlled migrations.
- Production deployments must not rely on Hibernate automatically modifying schema.

### Maintainability
- Category, location, promotion, and media behavior should be reusable across vertical sites.
- Business logic should not be duplicated unnecessarily between the main site and vertical UIs.

## 13. Current Scope Boundaries

The following are not assumed to be complete unless implemented and verified:
- Fully automated online payment gateway settlement.
- Advanced messaging/chat between buyer and seller.
- Delivery/logistics.
- Public forgot-password/email recovery.
- AI recommendation/ranking.
- Full enterprise analytics.
- Dedicated mobile applications.

## 14. Business Success Indicators

Potential product KPIs include:
- Active ads.
- New ads per day/week/month.
- Search-to-detail conversion.
- Contact actions per ad.
- Promotion purchase/request rate.
- Promotion revenue.
- Approval turnaround time.
- Repeat advertisers.
- Vertical-site traffic and conversion.
