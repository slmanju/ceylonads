package com.slmanju.ceylonads.payment.entity;

import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_customer", columnList = "customer_id")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One payment per promotion for this MVP; enforced at the database level too.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false, unique = true)
    private Promotion promotion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "payment_reference", nullable = false, length = 40, unique = true, updatable = false)
    private String paymentReference;

    // Snapshotted from Promotion.priceAmount at creation time. Never accepted from the client.
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "bank_reference", length = 100)
    private String bankReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_media_id")
    private Media receiptMedia;

    @Column(name = "customer_note", length = 500)
    private String customerNote;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by_account_id")
    private Long reviewedByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Payment() {
    }

    public Payment(Promotion promotion, Customer customer, String paymentReference, BigDecimal amount) {
        this.promotion = promotion;
        this.customer = customer;
        this.paymentReference = paymentReference;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public Promotion getPromotion() { return promotion; }
    public Customer getCustomer() { return customer; }
    public String getPaymentReference() { return paymentReference; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getBankReference() { return bankReference; }
    public Media getReceiptMedia() { return receiptMedia; }
    public String getCustomerNote() { return customerNote; }
    public String getAdminNote() { return adminNote; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Long getReviewedByAccountId() { return reviewedByAccountId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void attachReceipt(Media media) {
        this.receiptMedia = media;
        this.updatedAt = Instant.now();
    }

    public void submit(String bankReference, String customerNote) {
        this.bankReference = bankReference;
        this.customerNote = customerNote;
        this.status = PaymentStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // method/note are optional: a normal SUBMITTED->APPROVED review leaves the payment's
    // existing method/note untouched, while a manual/offline verification (no proof, e.g. cash
    // or a phone-arranged transfer) uses them to record how and why it was verified.
    public void approve(Long adminAccountId, PaymentMethod method, String note) {
        if (method != null) {
            this.paymentMethod = method;
        }
        if (note != null) {
            this.adminNote = note;
        }
        this.status = PaymentStatus.APPROVED;
        this.reviewedAt = Instant.now();
        this.reviewedByAccountId = adminAccountId;
        this.updatedAt = Instant.now();
    }

    public void reject(Long adminAccountId, String reason) {
        this.status = PaymentStatus.REJECTED;
        this.adminNote = reason;
        this.reviewedAt = Instant.now();
        this.reviewedByAccountId = adminAccountId;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
