package com.slmanju.ceylonads.payment.entity;

// BANK_TRANSFER is the default (and only) method a payment starts with; CASH/OTHER let an
// admin record how a manually-settled payment (e.g. cash at the office, phone-arranged) was
// actually paid when there's no bank receipt to upload. Card/PayHere/Stripe/PayPal are
// deliberately not implemented yet; adding one later is just another enum value.
public enum PaymentMethod {
    BANK_TRANSFER,
    CASH,
    OTHER
}
