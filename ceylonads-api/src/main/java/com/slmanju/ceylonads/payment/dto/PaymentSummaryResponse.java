package com.slmanju.ceylonads.payment.dto;

import com.slmanju.ceylonads.payment.entity.PaymentMethod;
import com.slmanju.ceylonads.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

// Lighter shape for list views (My Payments, admin payment list) where the full
// PaymentResponse detail - receipt URL, notes, timestamps - isn't needed per row.
public record PaymentSummaryResponse(
        Long id,
        String paymentReference,
        Long promotionId,
        Long adId,
        String adTitle,
        String promotionPlanName,
        Long customerId,
        String customerDisplayName,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String bankReference,
        Instant submittedAt,
        Instant createdAt) {
}
