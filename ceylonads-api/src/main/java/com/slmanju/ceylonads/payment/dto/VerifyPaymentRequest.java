package com.slmanju.ceylonads.payment.dto;

import com.slmanju.ceylonads.payment.entity.PaymentMethod;
import jakarta.validation.constraints.Size;

// Both fields are optional: a plain "Approve Payment" on a submitted receipt needs neither.
// They matter for manually verifying a PENDING payment with no proof (cash, phone-arranged,
// etc.), where the admin records how it was actually settled.
public record VerifyPaymentRequest(
        PaymentMethod paymentMethod,
        @Size(max = 500) String adminNote) {
}
