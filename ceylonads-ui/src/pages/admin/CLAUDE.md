# Admin Frontend Instructions

Applies to `pages/admin/` and the admin-only components under `components/`.

## Layout & structure

- Wrap admin pages with the existing `AdminLayout` component; do not build a parallel shell.
- Use `AdminPageHeader` for page titles/actions instead of ad-hoc headers.
- Reuse existing admin components before adding new ones:

```text
AdminAdCard
AdminCustomerRow
AdminPaymentReviewModal
StatCard
PaymentStatusBadge
PromotionStatusBadge
StatusBadge
ConfirmDialog
```

## Data & status

- Payment/promotion status values must match the backend enums exactly — see
  `ceylonads-api/.../payment/CLAUDE.md` for the lifecycle. Render them via the existing
  badge components rather than free-text or new color mappings.
- Destructive/approval actions (approve/reject payment, cancel promotion, delete) go
  through `ConfirmDialog`.

## Tables & lists

- Keep list pages pageable, matching the backend's pageable endpoints.
- Tables must degrade to a usable stacked/scrollable layout below ~768px — do not let
  admin tables force horizontal page scroll.

## Authorization

- Admin routes rely on the shared route-protection mechanism (see root frontend
  `CLAUDE.md`) — do not create a separate admin auth/session flow.
