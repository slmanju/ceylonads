# CeylonAds Tuition UI Instructions

These rules apply to the tuition frontend inside the existing CeylonAds monorepo.

## Purpose

The tuition frontend is a specialized vertical UI over the existing CeylonAds platform.

It must reuse the same backend, accounts, authentication, ads, categories, locations, media, favourites and promotion capabilities.

Conceptually:

```text
CeylonAds Monorepo
├── ceylonads-api
├── ceylonads-ui
└── ceylonads-tuition-ui
```

Do not create a separate tuition backend.

---

## Architecture

The tuition UI is a dedicated frontend over the shared CeylonAds API.

```text
                 CeylonAds API
                      │
          ┌───────────┴───────────┐
          │                       │
   CeylonAds UI             Tuition UI
```

Both frontends work with the same underlying data.

Do not duplicate business data or create frontend-only copies of backend domain state.

---

## Stack

Use the versions already declared by the project.

Expected stack:

```text
React
TypeScript
Vite
React Router
Axios
```

Do not change major framework or dependency versions unless explicitly requested.

Prefer existing project dependencies and conventions.

---

## Project Structure

Keep tuition-specific frontend code inside:

```text
ceylonads-tuition-ui/
```

Recommended structure:

```text
src/
├── api/
├── auth/
├── components/
├── features/
├── layouts/
├── pages/
├── routes/
├── types/
├── utils/
└── assets/
```

Do not create empty folders merely to satisfy this structure.

Organize code by feature where practical.

---

## Shared CeylonAds Backend

The tuition UI uses the same CeylonAds backend as the main site.

Reuse existing capabilities for:

```text
authentication
accounts
ads
categories
dynamic attributes
locations
media
favourites
promotions
search
posting ads
editing ads
```

Do not introduce a tuition-specific backend when the shared CeylonAds model can represent the required capability.

Backend changes, when explicitly requested, belong in `ceylonads-api`.

---

## Shared Accounts and Authentication

There is one CeylonAds account system.

A user must not need separate credentials for tuition.

```text
CeylonAds Account
      │
      ├── Main CeylonAds
      └── Tuition UI
```

Reuse the existing JWT authentication behaviour.

Do not create:

```text
tuition_users
tuition_accounts
separate tuition tokens
```

Respect existing roles and authorization behaviour.

---

## API Client

Follow the same API conventions used by the main CeylonAds frontend.

Use environment configuration such as:

```text
VITE_API_BASE_URL
```

Do not hardcode:

```text
localhost URLs
Cloud Run URLs
production domains
bucket URLs
```

Keep API calls centralized rather than scattering Axios calls throughout components.

Handle authentication headers consistently.

---

## API Models

TypeScript types should accurately represent backend DTO contracts.

Prefer existing naming conventions such as:

```text
AdResponse
CategoryResponse
LocationResponse
AttributeDefinition
```

Reuse shared types if a shared frontend package already exists.

Do not create multiple incompatible models for the same API response.

Avoid `any` unless genuinely unavoidable.

---

## Categories

Use backend category data.

Never hardcode database category IDs.

Prefer stable category slugs or other backend-supported identifiers where needed.

The tuition UI should expose only tuition-relevant category structures to users.

Do not unnecessarily display the entire generic CeylonAds taxonomy.

---

## Dynamic Attributes

Tuition-specific metadata should use the CeylonAds dynamic attribute system where supported.

Examples can include:

```text
subject
grade
medium
syllabus
class type
delivery mode
```

Do not duplicate backend attribute definitions in frontend code.

Render forms and filters from backend metadata where practical.

Keep backend attribute keys intact.

---

## Locations

Use the shared CeylonAds location data.

Do not create a separate tuition copy of Sri Lankan location master data.

Follow the existing hierarchy where applicable:

```text
Province
District
City
```

Location should remain optional when the underlying ad type supports online-only tuition.

---

## Search

The tuition UI should provide one consistent search experience.

Avoid creating separate competing behaviours such as:

```text
browse mode
category navigation mode
search mode
```

when the same backend search can serve them.

Search controls must correspond to real backend query capabilities.

Do not fetch all ads and filter them client-side.

Use backend filtering and pagination.

---

## Tuition UX

The tuition frontend should feel like a dedicated Sri Lankan tuition platform, not a generic classified site with a different logo.

Prioritize tuition-specific discovery such as:

```text
subject
grade
medium
syllabus
location
online / physical
class type
```

when the data and APIs support them.

Keep the experience simple and focused.

---

## Homepage

The homepage should prioritize discovery.

Typical flow:

```text
Header
↓
Hero + Search
↓
Quick Tuition Categories
↓
Featured Classes
↓
Browse by Subject
↓
Online Classes
↓
Popular Locations
↓
Footer
```

Only show sections that can be backed by real data.

Do not add fake:

```text
statistics
reviews
student counts
teacher counts
success metrics
```

---

## Header and Navigation

Keep navigation focused on tuition.

Typical navigation can include:

```text
Home
Classes
Tutors
Online Classes
Districts
Post Tuition Ad
Account
```

Navigation should remain simple on mobile.

Do not overload the header with unnecessary category links.

---

## Ad Cards

Tuition ad cards should prioritize useful information.

Show available fields such as:

```text
image
title
fee
subject
grade
medium
location
teacher/seller
online indicator
featured indicator
```

Do not show empty placeholders.

Bad:

```text
Medium: -
Location: -
```

If a field is unavailable, omit it.

---

## Ad Detail

Use the shared CeylonAds ad-detail API.

Present the response using a tuition-focused layout.

Typical sections:

```text
Title
Gallery
Fee
Class Details
Description
Location
Seller / Tutor Contact
Related Ads
```

Dynamic tuition attributes should be grouped clearly under something like:

```text
Class Details
```

Do not invent information not returned by the backend.

---

## Posting Ads

Use the shared CeylonAds ad creation flow.

The tuition UI may provide a more focused UX while using the same backend contract.

Use backend-provided:

```text
categories
attribute definitions
locations
validation
media handling
```

Do not duplicate backend validation rules unnecessarily.

Display backend validation errors clearly.

---

## Media

Follow the existing CeylonAds media strategy.

Do not hardcode storage bucket URLs.

Use backend-returned URLs or the shared media resolver already used by CeylonAds.

Provide clean fallbacks for ads without images.

Lazy-load non-critical images where appropriate.

---

## Favourites

Reuse the shared CeylonAds favourites capability.

A tuition ad favourited in the tuition UI represents the same favourite on the main platform.

Do not create separate tuition favourite storage.

---

## Promotions

Reuse the common CeylonAds promotion model.

Promotion-related UI should use real backend promotion data.

Do not implement frontend-only promotion logic.

Tuition-specific promotion behaviour, when introduced, should extend the shared promotion architecture rather than creating a separate promotion system.

---

## State and Data Fetching

Follow existing CeylonAds frontend conventions.

Avoid unnecessary global state.

Cache relatively stable reference data where appropriate:

```text
categories
locations
attribute definitions
```

Avoid repeated network calls for the same reference data.

Do not cache user-sensitive data inappropriately.

---

## Routing

Use React Router.

Prefer clean public URLs.

Examples:

```text
/
 /classes
 /classes/:slug
 /online-classes
 /tutors
 /districts
 /post-ad
 /my-ads
 /account
 /login
```

Use slugs for public-facing resources when supported.

Avoid exposing raw database IDs unnecessarily.

---

## Styling

Keep the design:

```text
modern
clean
education-focused
trustworthy
responsive
```

Use consistent:

```text
spacing
typography
border radius
shadows
button styles
form controls
```

Avoid:

```text
oversized icons
excessive gradients
too many chips
dashboard-style clutter
unnecessary animations
```

Use approved project assets rather than recreating them unnecessarily.

---

## Responsive Design

All pages must work well on:

```text
mobile
tablet
desktop
```

Pay particular attention to:

```text
hero search
navigation
filters
ad cards
image gallery
forms
```

Design mobile behaviour intentionally rather than patching desktop layouts later.

---

## Accessibility

Use semantic HTML.

Include:

```text
input labels
keyboard-accessible controls
visible focus states
appropriate alt text
proper button/link semantics
accessible validation messages
```

Maintain suitable contrast.

---

## Error Handling

Provide clear user-facing states for:

```text
loading
empty results
network failures
404
unauthorized access
validation errors
media failures
```

Do not expose raw stack traces or backend technical messages to users.

---

## Performance

Avoid:

```text
duplicate API calls
client-side filtering of full datasets
large unoptimized images
unnecessary rerenders
repeated loading of category/location trees
```

Use backend pagination.

Lazy-load non-critical content where appropriate.

Keep the initial homepage load lightweight.

---

## SEO

Use meaningful public URLs and metadata.

Examples:

```text
Tuition Classes in Sri Lanka | ezClass
A/L Mathematics Classes | ezClass
Online Tuition Classes | ezClass
```

Use descriptive page titles and meta descriptions for public routes.

Do not introduce a major rendering architecture change solely for SEO unless explicitly requested.

---

## Code Quality

Prefer straightforward, maintainable code.

Do not:

```text
over-engineer
create speculative abstractions
rewrite working code without reason
introduce unused frameworks
duplicate backend domain logic
```

Keep components focused.

Extract reusable components when there is real reuse.

---

## Reuse vs Duplication

Reuse existing CeylonAds frontend behaviour where practical, especially:

```text
authentication
API handling
media
categories
locations
favourites
ad DTOs
error handling
```

Do not perform a large refactor of `ceylonads-ui` merely to eliminate small duplication.

If shared frontend code becomes substantial and stable, extract it deliberately into a shared package as a separate task.

---

## Testing and Verification

Use the existing frontend testing/build conventions.

At minimum, ensure the production build succeeds.

Verify key flows when they are affected:

```text
homepage
navigation
search
filters
ad detail
authentication
posting ads
favourites
responsive layout
error handling
```

Do not leave TypeScript or build errors.

---

## General Rule

The tuition site is a **specialized presentation and discovery experience built on top of CeylonAds**.

When deciding where functionality belongs:

```text
Generic marketplace capability
→ CeylonAds backend/shared platform

Tuition-specific presentation
→ Tuition UI

Reusable frontend infrastructure
→ Shared only when justified
```

Keep this separation clear so future verticals can reuse the CeylonAds platform without accumulating unnecessary coupling.
