package com.slmanju.ceylonads.payment.mapper;

import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.storage.MediaStorage;
import com.slmanju.ceylonads.payment.dto.PaymentResponse;
import com.slmanju.ceylonads.payment.dto.PaymentSummaryResponse;
import com.slmanju.ceylonads.payment.entity.Payment;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    private final MediaStorage storage;

    public PaymentMapper(MediaStorage storage) {
        this.storage = storage;
    }

    public PaymentResponse toResponse(Payment payment) {
        Promotion promotion = payment.getPromotion();
        Customer customer = payment.getCustomer();
        Media receipt = payment.getReceiptMedia();
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentReference(),
                promotion.getId(),
                promotion.getAd().getId(),
                promotion.getAd().getTitle(),
                promotion.getPlan().getName(),
                promotion.getPlan().getPlacementType(),
                customer.getId(),
                customer.getDisplayName(),
                customer.getPhone(),
                customer.getAccount().getEmail(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getBankReference(),
                receipt != null ? storage.publicUrl(receipt.getStorageKey()) : null,
                payment.getCustomerNote(),
                payment.getAdminNote(),
                payment.getSubmittedAt(),
                payment.getReviewedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }

    public PaymentSummaryResponse toSummary(Payment payment) {
        Promotion promotion = payment.getPromotion();
        return new PaymentSummaryResponse(
                payment.getId(),
                payment.getPaymentReference(),
                promotion.getId(),
                promotion.getAd().getId(),
                promotion.getAd().getTitle(),
                promotion.getPlan().getName(),
                payment.getCustomer().getId(),
                payment.getCustomer().getDisplayName(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getBankReference(),
                payment.getSubmittedAt(),
                payment.getCreatedAt());
    }
}
