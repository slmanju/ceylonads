package com.slmanju.ceylonads.payment.repository;

import com.slmanju.ceylonads.payment.entity.Payment;
import com.slmanju.ceylonads.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // PaymentMapper.toSummary touches promotion/promotion.ad/promotion.plan/customer for every
    // row; fetch them here instead of lazily per payment when mapping a list to
    // PaymentSummaryResponse.
    @EntityGraph(attributePaths = {"promotion", "promotion.ad", "promotion.plan", "customer"})
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"promotion", "promotion.ad", "promotion.plan", "customer"})
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    @EntityGraph(attributePaths = {"promotion", "promotion.ad", "promotion.plan", "customer"})
    List<Payment> findAllByOrderByCreatedAtDesc();

    long countByStatus(PaymentStatus status);

    boolean existsByPaymentReference(String paymentReference);
}
