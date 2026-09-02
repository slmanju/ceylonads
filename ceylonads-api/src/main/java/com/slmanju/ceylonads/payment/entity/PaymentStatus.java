package com.slmanju.ceylonads.payment.entity;

public enum PaymentStatus {
    // Payment record exists but the customer has not submitted transfer proof.
    PENDING,
    // Customer has entered a bank reference and uploaded a receipt; awaiting admin review.
    SUBMITTED,
    // Admin confirmed the transfer; the related promotion is activated.
    APPROVED,
    // Admin rejected the submitted proof; the customer may correct and resubmit.
    REJECTED,
    // Customer or admin cancelled the payment before approval.
    CANCELLED
}
