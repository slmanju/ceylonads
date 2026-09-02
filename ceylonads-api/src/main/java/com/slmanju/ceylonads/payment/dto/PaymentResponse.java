package com.slmanju.ceylonads.payment.dto;

import com.slmanju.ceylonads.payment.entity.PaymentMethod;
import com.slmanju.ceylonads.payment.entity.PaymentStatus;
import com.slmanju.ceylonads.promotion.entity.PlacementType;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        String paymentReference,
        Long promotionId,
        Long adId,
        String adTitle,
        String promotionPlanName,
        PlacementType placementType,
        Long customerId,
        String customerDisplayName,
        String customerPhone,
        String customerEmail,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String bankReference,
        String receiptUrl,
        String customerNote,
        String adminNote,
        Instant submittedAt,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt) {
}
