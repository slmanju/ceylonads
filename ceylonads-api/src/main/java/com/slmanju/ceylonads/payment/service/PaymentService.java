package com.slmanju.ceylonads.payment.service;

import com.slmanju.ceylonads.auth.entity.Account;
import com.slmanju.ceylonads.auth.repository.AccountRepository;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.customer.service.CustomerService;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.service.MediaService;
import com.slmanju.ceylonads.payment.config.BankTransferProperties;
import com.slmanju.ceylonads.payment.dto.BankTransferDetailsResponse;
import com.slmanju.ceylonads.payment.dto.PaymentResponse;
import com.slmanju.ceylonads.payment.dto.PaymentSummaryResponse;
import com.slmanju.ceylonads.payment.dto.RejectPaymentRequest;
import com.slmanju.ceylonads.payment.dto.SubmitPaymentRequest;
import com.slmanju.ceylonads.payment.dto.VerifyPaymentRequest;
import com.slmanju.ceylonads.payment.entity.Payment;
import com.slmanju.ceylonads.payment.entity.PaymentMethod;
import com.slmanju.ceylonads.payment.entity.PaymentStatus;
import com.slmanju.ceylonads.payment.mapper.PaymentMapper;
import com.slmanju.ceylonads.payment.repository.PaymentRepository;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.event.PromotionCreatedEvent;
import com.slmanju.ceylonads.promotion.service.PromotionService;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Owns the bank-transfer payment lifecycle (create -> submit -> approve/reject -> cancel).
 * Promotion activation itself is never duplicated here: {@link #approve(Long, String)} calls
 * straight into {@link PromotionService#activate(Long)}, the same method Phase 2's manual admin
 * activation used.
 */
@Service
public class PaymentService {

    private static final Set<PaymentStatus> RECEIPT_EDITABLE = Set.of(PaymentStatus.PENDING, PaymentStatus.REJECTED);
    private static final Set<PaymentStatus> SUBMITTABLE = Set.of(PaymentStatus.PENDING, PaymentStatus.REJECTED);
    private static final Set<PaymentStatus> CANCELLABLE = Set.of(PaymentStatus.PENDING, PaymentStatus.REJECTED);
    // PENDING is included so an admin can manually verify a cash/phone-arranged/offline
    // settlement that never went through the customer's upload-receipt-and-submit flow.
    // Proof (a SUBMITTED receipt) is supporting evidence, not a precondition for verification.
    private static final Set<PaymentStatus> APPROVABLE = Set.of(PaymentStatus.PENDING, PaymentStatus.SUBMITTED);

    private final PaymentRepository payments;
    private final PromotionService promotionService;
    private final CustomerService customerService;
    private final MediaService mediaService;
    private final AccountRepository accounts;
    private final PaymentMapper mapper;
    private final BankTransferProperties bankTransferProperties;

    public PaymentService(
            PaymentRepository payments,
            PromotionService promotionService,
            CustomerService customerService,
            MediaService mediaService,
            AccountRepository accounts,
            PaymentMapper mapper,
            BankTransferProperties bankTransferProperties) {
        this.payments = payments;
        this.promotionService = promotionService;
        this.customerService = customerService;
        this.mediaService = mediaService;
        this.accounts = accounts;
        this.mapper = mapper;
        this.bankTransferProperties = bankTransferProperties;
    }

    /**
     * Reacts to a brand-new promotion by creating its PENDING bank-transfer payment, snapshotting
     * the amount from the promotion so the customer's browser never gets a say in what's payable.
     * Fired synchronously from within PromotionService's own transaction (see
     * {@link PromotionCreatedEvent}), so a thrown exception here rolls the promotion back too.
     */
    @EventListener
    @Transactional
    public void onPromotionCreated(PromotionCreatedEvent event) {
        Promotion promotion = event.promotion();
        Payment payment = new Payment(promotion, promotion.getCustomer(), generateReference(), promotion.getPriceAmount());
        payments.save(payment);
    }

    @Transactional(readOnly = true)
    public BankTransferDetailsResponse bankTransferDetails() {
        return bankTransferProperties.toResponse();
    }

    @Transactional(readOnly = true)
    public List<PaymentSummaryResponse> mine(String username) {
        Customer customer = customerService.requireByUsername(username);
        return payments.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream().map(mapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getOwned(Long id, String username) {
        Customer customer = customerService.requireByUsername(username);
        return mapper.toResponse(requireOwned(id, customer.getId()));
    }

    @Transactional
    public PaymentResponse uploadReceipt(Long id, String username, MultipartFile file) throws IOException {
        Customer customer = customerService.requireByUsername(username);
        Payment payment = requireOwned(id, customer.getId());
        if (!RECEIPT_EDITABLE.contains(payment.getStatus())) {
            throw new BadRequestException("A receipt can only be uploaded before a payment is submitted for review");
        }
        Media media = mediaService.storePaymentReceipt(file);
        payment.attachReceipt(media);
        return mapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse submit(Long id, String username, SubmitPaymentRequest request) {
        Customer customer = customerService.requireByUsername(username);
        Payment payment = requireOwned(id, customer.getId());
        if (!SUBMITTABLE.contains(payment.getStatus())) {
            throw new BadRequestException("Only a pending or rejected payment can be submitted");
        }
        if (payment.getReceiptMedia() == null) {
            throw new BadRequestException("Please upload your bank transfer receipt before submitting");
        }
        payment.submit(request.bankReference().trim(), trimToNull(request.customerNote()));
        return mapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse cancelOwned(Long id, String username) {
        Customer customer = customerService.requireByUsername(username);
        Payment payment = requireOwned(id, customer.getId());
        if (!CANCELLABLE.contains(payment.getStatus())) {
            throw new BadRequestException("This payment can no longer be cancelled");
        }
        payment.cancel();
        promotionService.cancelOwned(payment.getPromotion().getId(), username);
        return mapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentSummaryResponse> adminList(PaymentStatus statusFilter) {
        List<Payment> results = statusFilter == null
                ? payments.findAllByOrderByCreatedAtDesc()
                : payments.findByStatusOrderByCreatedAtDesc(statusFilter);
        return results.stream().map(mapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse adminGet(Long id) {
        return mapper.toResponse(requireAny(id));
    }

    @Transactional(readOnly = true)
    public long countByStatus(PaymentStatus status) {
        return payments.countByStatus(status);
    }

    /**
     * The core Phase 3 transaction: a PENDING or SUBMITTED payment -> APPROVED, and (via the same
     * activate() PromotionService already exposes for Phase 2 manual admin activation)
     * PENDING_PAYMENT promotion -> ACTIVE. Both happen in one transaction, so a failed activation
     * - e.g. the ad was deactivated in the meantime - leaves the payment unapproved, not silently
     * APPROVED. PENDING is approvable too so an admin can manually verify a cash/phone-arranged/
     * offline settlement that has no uploaded receipt and was never customer-submitted; request
     * is optional and only matters for that manual-verification case.
     */
    @Transactional
    public PaymentResponse approve(Long id, String adminUsername, VerifyPaymentRequest request) {
        Payment payment = requireAny(id);
        if (!APPROVABLE.contains(payment.getStatus())) {
            throw new BadRequestException("Only a pending or submitted payment can be approved");
        }
        PaymentMethod method = request != null ? request.paymentMethod() : null;
        String note = request != null ? trimToNull(request.adminNote()) : null;
        payment.approve(requireAccountId(adminUsername), method, note);
        promotionService.activate(payment.getPromotion().getId());
        return mapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse reject(Long id, String adminUsername, RejectPaymentRequest request) {
        Payment payment = requireAny(id);
        if (payment.getStatus() != PaymentStatus.SUBMITTED) {
            throw new BadRequestException("Only a submitted payment can be rejected");
        }
        payment.reject(requireAccountId(adminUsername), request.reason().trim());
        return mapper.toResponse(payment);
    }

    private String generateReference() {
        String candidate;
        int attempts = 0;
        do {
            candidate = "CA-PAY-" + Year.now() + "-" + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            attempts++;
        } while (payments.existsByPaymentReference(candidate) && attempts < 20);
        return candidate;
    }

    private Long requireAccountId(String username) {
        Account account = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return account.getId();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Payment requireOwned(Long id, Long customerId) {
        Payment payment = requireAny(id);
        if (!payment.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("Not your payment");
        }
        return payment;
    }

    private Payment requireAny(Long id) {
        return payments.findById(id).orElseThrow(() -> new NotFoundException("Payment not found"));
    }
}
