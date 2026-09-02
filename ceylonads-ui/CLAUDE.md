# CeylonAds Frontend Instructions

These rules apply to work inside `ceylonads-ui/`.

## Stack

Use the existing stack:

```text
React
Vite
TypeScript
React Router
Axios
```

Do not replace the framework or introduce a heavyweight UI framework without explicit instruction.

## Backend contract

Swagger/OpenAPI is the source of truth:

```text
http://localhost:8080/v3/api-docs
```

For frontend tasks:

1. read relevant OpenAPI paths/schemas
2. update TypeScript types/API functions
3. implement the feature
4. build and verify

Do not recursively scan the Java backend.
Only inspect one specific Java class when OpenAPI cannot answer a specific question.

## API layer

Keep HTTP calls centralized using the existing API modules.
Do not scatter raw Axios calls across pages/components.

Use `VITE_API_BASE_URL`; do not hardcode backend URLs throughout the app.
JWT is attached through the existing auth/Axios mechanism.

## Routing / authorization

Preserve route protection.
Roles are `CUSTOMER` and `ADMIN`.
Frontend checks improve UX; backend authorization is authoritative.
Do not create separate authentication state for admin.

## UI identity

CeylonAds is a classifieds marketplace.

Theme:

```text
teal
slate
warm neutral backgrounds
white cards
subtle borders/shadows
```

Avoid blue-heavy ecommerce styling, shopping-cart visual language, excessive gradients, glassmorphism, giant hero sections, and overly rounded pill controls.

Use color mainly for actions/status, not every heading/icon.
Do not add heart/favorite controls unless the feature is explicitly implemented.

## Components

Reuse existing components before adding new ones.
Do not duplicate ad-card/search/form implementations per page when shared components fit.

## Responsive behavior

Keep layouts usable around:

```text
390px
768px
1440px
```

Requirements:

- no page-level horizontal overflow
- mobile filters use the existing drawer/panel pattern
- forms remain touch-friendly
- marketplace cards retain usable image ratios

Admin-specific table/layout rules live in `pages/admin/CLAUDE.md`.

## State handling

API-driven screens handle loading, success, empty, validation errors, 401, 403, 404, and network/server errors.

Do not leave blank screens.

## Accessibility

Use semantic HTML, real buttons/links/labels, visible focus states, and useful alt text.

## Before finishing

Run:

```bash
npm run build
```

Also run existing lint/tests when relevant.
Fix TypeScript/build failures and keep the final report concise.
