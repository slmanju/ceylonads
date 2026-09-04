package com.slmanju.ceylonads.promotion.entity;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.media.entity.Media;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "promotions", indexes = {
        @Index(name = "idx_promotions_status_ends", columnList = "status,ends_at"),
        @Index(name = "idx_promotions_customer", columnList = "customer_id"),
        @Index(name = "idx_promotions_ad", columnList = "ad_id")
})
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private PromotionKind kind;

    // Required for AD_PROMOTION, null for BANNER_PROMOTION.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id")
    private Ad ad;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_plan_id", nullable = false)
    private PromotionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionStatus status = PromotionStatus.PENDING_PAYMENT;

    // Snapshotted from the plan at creation time so a later price/duration change on
    // PromotionPlan never retroactively alters a promotion the customer already agreed to.
    @Column(name = "price_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    // Required for BANNER_PROMOTION, null for AD_PROMOTION.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_media_id")
    private Media bannerMedia;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    // True when an admin explicitly waived payment for a complimentary promotion. The price
    // snapshot above is still populated for the audit trail even though nothing was charged.
    @Column(name = "payment_waived", nullable = false)
    private boolean paymentWaived = false;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Promotion() {
    }

    public Promotion(
            Ad ad,
            Customer customer,
            PromotionPlan plan,
            BigDecimal priceAmount,
            int durationDays,
            PromotionStatus initialStatus,
            boolean paymentWaived) {
        this.kind = PromotionKind.AD_PROMOTION;
        this.ad = ad;
        this.customer = customer;
        this.plan = plan;
        this.priceAmount = priceAmount;
        this.durationDays = durationDays;
        this.status = initialStatus;
        this.paymentWaived = paymentWaived;
    }

    public static Promotion forBanner(
            Customer customer,
            PromotionPlan plan,
            BigDecimal priceAmount,
            int durationDays,
            Media bannerMedia,
            String targetUrl,
            PromotionStatus initialStatus,
            boolean paymentWaived) {
        Promotion promotion = new Promotion();
        promotion.kind = PromotionKind.BANNER_PROMOTION;
        promotion.customer = customer;
        promotion.plan = plan;
        promotion.priceAmount = priceAmount;
        promotion.durationDays = durationDays;
        promotion.bannerMedia = bannerMedia;
        promotion.targetUrl = targetUrl;
        promotion.status = initialStatus;
        promotion.paymentWaived = paymentWaived;
        return promotion;
    }

    public Long getId() { return id; }
    public PromotionKind getKind() { return kind; }
    public Ad getAd() { return ad; }
    public Customer getCustomer() { return customer; }
    public PromotionPlan getPlan() { return plan; }
    public PromotionStatus getStatus() { return status; }
    public BigDecimal getPriceAmount() { return priceAmount; }
    public int getDurationDays() { return durationDays; }
    public Media getBannerMedia() { return bannerMedia; }
    public String getTargetUrl() { return targetUrl; }
    public boolean isPaymentWaived() { return paymentWaived; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void activate() {
        this.status = PromotionStatus.ACTIVE;
        this.startsAt = Instant.now();
        this.endsAt = this.startsAt.plus(Duration.ofDays(durationDays));
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = PromotionStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void expire() {
        this.status = PromotionStatus.EXPIRED;
        this.updatedAt = Instant.now();
    }

    // DEV/seed-only: directly sets lifecycle fields for deterministic sample data - e.g. a
    // promotion that already ended, or one scheduled to start in the future - neither of which
    // activate() can express since it always pins startsAt to now(). Never called by application
    // business logic; mirrors Ad#seedExpiryOverride's equivalent purpose.
    public void seedLifecycleOverride(PromotionStatus status, Instant startsAt, Instant endsAt) {
        this.status = status;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.updatedAt = Instant.now();
    }
}
