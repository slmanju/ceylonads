# Payment & Promotion Instructions

Applies to `payment/` and the payment-related parts of `promotion/`.

## Workflow

Manual bank-transfer only (no payment gateway integration):

```text
1. Customer creates a Promotion -> PromotionCreatedEvent
2. PaymentService.onPromotionCreated creates a PENDING Payment, snapshotting the
   promotion's price so the client can never influence what's payable
3. Customer submits bank reference + proof -> Payment moves to SUBMITTED
4. Admin reviews -> APPROVED or REJECTED
5. Approval activates the Promotion (PENDING_PAYMENT -> ACTIVE) in the same
   transaction as the payment approval
```

## Status enums

```text
PaymentStatus:   PENDING, SUBMITTED, APPROVED, REJECTED, CANCELLED
PromotionStatus: PENDING_PAYMENT, ACTIVE, EXPIRED, CANCELLED
```

## Key rules

- Promotion activation is never duplicated: payment approval calls straight into
  `PromotionService.activate(Long)`, the same method used for manual admin activation.
- Keep payment approval and promotion activation in one `@Transactional` boundary so a
  failed activation (e.g. the ad no longer exists) rolls the payment approval back too.
- Bank account details (`app.payment.bank-transfer.*`) are environment-driven; local/demo
  values are intentionally fake. Never hardcode real account details.
- Do not add a payment gateway/PSP integration unless explicitly requested — this is a
  deliberate MVP scope decision, not a placeholder to fill in.
