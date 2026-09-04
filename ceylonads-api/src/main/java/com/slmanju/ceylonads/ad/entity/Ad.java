package com.slmanju.ceylonads.ad.entity;

import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.customer.entity.Customer;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "ads", indexes = {
        @Index(name = "idx_ads_status_created", columnList = "status,created_at"),
        @Index(name = "idx_ads_category", columnList = "category_id"),
        @Index(name = "idx_ads_category_status_created", columnList = "category_id,status,created_at"),
        @Index(name = "idx_ads_source_channel_status_created", columnList = "source_channel,status,created_at"),
        @Index(name = "idx_ads_source_channel_status_expires", columnList = "source_channel,status,expires_at")
})
public class Ad {

    // Tuition's free-listing lifetime (see TuitionClassService/CLAUDE.md for the product rule).
    // Only ever applied when sourceChannel == TUITION - MAIN_SITE/BOARDING ads never populate
    // expiresAt at all (see approve() below), so this constant has no effect on those channels.
    private static final Duration TUITION_FREE_LISTING_DURATION = Duration.ofDays(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Customer seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdStatus status = AdStatus.PENDING_REVIEW;

    // Which storefront/vertical owns this listing (see SourceChannel). Defaults to MAIN_SITE so
    // every normal AdService.create() call - the only path that creates ads through the main
    // public API - never needs to say so explicitly; DEV seeders reassign this via
    // assignSourceChannel for the Tuition/Boarding datasets.
    @Enumerated(EnumType.STRING)
    @Column(name = "source_channel", nullable = false, length = 20)
    private SourceChannel sourceChannel = SourceChannel.MAIN_SITE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    // Public-visibility deadline, TUITION-only (see approve()/renew()/extendExpiryToAtLeast()
    // below) - null forever for MAIN_SITE/BOARDING, meaning "never expires" wherever this is
    // checked (see AdSpecifications.notExpired). Authoritative for Tuition public visibility
    // alongside status == ACTIVE; a scheduled job eventually flips a stale ACTIVE+past-expiresAt
    // ad to EXPIRED, but every public read also checks this directly so visibility is correct
    // immediately at the deadline, not just after the next scheduler run.
    @Column(name = "expires_at")
    private Instant expiresAt;

    // Moderation audit trail: who reviewed this ad (approved/rejected) and when. Mirrors
    // Payment.reviewedByAccountId/reviewedAt - a raw account id rather than a relation, since the
    // reviewer is never loaded as part of an Ad. Null until the first approve/reject. "Created by"
    // needs no separate field: it's already the seller's account (see getSeller().getAccount()),
    // which covers self-approval audit (a Moderator can be both).
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by_account_id")
    private Long reviewedByAccountId;

    // Ad-specific contact override: the person posting isn't always who buyers should contact
    // (e.g. a family member posting for someone, a business, an agent). Each field is
    // independently optional and falls back to the seller's account contact when null - see
    // AdMapper.resolveContact.
    @Column(name = "contact_name", length = 120)
    private String contactName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "whatsapp_number", length = 30)
    private String whatsappNumber;

    protected Ad() {
    }

    public Ad(String title, String description, BigDecimal price, Category category, Customer seller) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.seller = seller;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Category getCategory() { return category; }
    public Customer getSeller() { return seller; }
    public AdStatus getStatus() { return status; }
    public SourceChannel getSourceChannel() { return sourceChannel; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Long getReviewedByAccountId() { return reviewedByAccountId; }
    public String getContactName() { return contactName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getWhatsappNumber() { return whatsappNumber; }

    // Reassigns the storefront/vertical that owns this ad. Only meant for DEV/seed classification
    // (e.g. tagging the Tuition sample dataset) - normal application flows never move an ad
    // between channels, and public request DTOs never expose this.
    public void assignSourceChannel(SourceChannel sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public void updateContact(String contactName, String phoneNumber, String whatsappNumber) {
        this.contactName = contactName;
        this.phoneNumber = phoneNumber;
        this.whatsappNumber = whatsappNumber;
    }

    public void update(String title, String description, BigDecimal price, Category category) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.updatedAt = Instant.now();
        this.status = AdStatus.PENDING_REVIEW;
        // TUITION is the one exception: a normal content edit is not a renewal and must not reset
        // the free-listing clock or force a fresh 30-day window on re-approval (see approve()
        // below, which only sets publishedAt/expiresAt when publishedAt is still null) - editing
        // still returns the listing to PENDING_REVIEW for re-moderation like every other channel,
        // it just doesn't touch either timestamp while that happens.
        if (this.sourceChannel != SourceChannel.TUITION) {
            this.publishedAt = null;
        }
    }

    // reviewerAccountId is nullable: seed data backdates ads straight into ACTIVE/REJECTED with no
    // real reviewer behind it. publishedAt/expiresAt are only set the first time this ad ever
    // becomes ACTIVE (publishedAt still null): moderation waiting time must not consume the
    // tutor's public listing lifetime, and a later re-approval (after an edit) must not grant a
    // fresh expiry window - see update() above for why publishedAt survives a TUITION edit.
    public void approve(Long reviewerAccountId) {
        this.status = AdStatus.ACTIVE;
        if (this.publishedAt == null) {
            this.publishedAt = Instant.now();
            if (this.sourceChannel == SourceChannel.TUITION) {
                this.expiresAt = this.publishedAt.plus(TUITION_FREE_LISTING_DURATION);
            }
        }
        this.updatedAt = Instant.now();
        this.reviewedAt = Instant.now();
        this.reviewedByAccountId = reviewerAccountId;
    }

    public void reject(Long reviewerAccountId) {
        this.status = AdStatus.REJECTED;
        this.updatedAt = Instant.now();
        this.reviewedAt = Instant.now();
        this.reviewedByAccountId = reviewerAccountId;
    }

    public void deactivate() {
        this.status = AdStatus.DEACTIVATED;
        this.updatedAt = Instant.now();
    }

    // TUITION-only lifecycle action (see TuitionClassService#renew for the eligibility check this
    // assumes has already passed - EXPIRED, or ACTIVE within 7 days of expiring): an EXPIRED
    // listing restarts a fresh 30-day window from now; a still-ACTIVE listing simply extends its
    // existing expiresAt by the same 30 days, so back-to-back renewals can never stack unboundedly
    // far into the future (each renewal only becomes possible again once within 7 days of the new
    // deadline).
    public void renew(int extensionDays) {
        Instant now = Instant.now();
        if (this.status == AdStatus.EXPIRED) {
            this.status = AdStatus.ACTIVE;
            this.expiresAt = now.plus(Duration.ofDays(extensionDays));
        } else {
            this.expiresAt = this.expiresAt.plus(Duration.ofDays(extensionDays));
        }
        this.updatedAt = now;
    }

    // The paid-duration guarantee: a promotion must never outlive the listing it was bought for.
    // Only ever raises expiresAt, never lowers it - an existing longer expiry (from a previous,
    // longer-running promotion) is left untouched, matching the "expiresAt >= the latest end time
    // of every promotion that needs this listing visible" invariant across multiple promotions.
    public void extendExpiryToAtLeast(Instant minimumExpiry) {
        if (minimumExpiry != null && (this.expiresAt == null || this.expiresAt.isBefore(minimumExpiry))) {
            this.expiresAt = minimumExpiry;
            this.updatedAt = Instant.now();
        }
    }

    // Bypasses the Instant.now() default set at construction. Only meant for backdating
    // timestamps in seed/test data so Newest/Oldest sorting has realistic spread to exercise;
    // normal application flows never need to move createdAt away from the moment of creation. When
    // an expiresAt is already set (TUITION, approved), it's shifted by the same amount as
    // publishedAt so the seeded "days remaining" stays exactly what approve() originally computed.
    public void backdateCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        if (this.publishedAt != null) {
            if (this.expiresAt != null) {
                Duration remainingValidity = Duration.between(this.publishedAt, this.expiresAt);
                this.expiresAt = createdAt.plus(remainingValidity);
            }
            this.publishedAt = createdAt;
        }
    }

    // DEV/seed-only: overrides the natural expiry state so curated Tuition sample data can
    // deterministically exercise "expires soon"/"already expired" UI states without waiting real
    // days. Never called by application business logic - see SampleDataSeeder.
    public void seedExpiryOverride(Instant expiresAt, AdStatus status) {
        this.expiresAt = expiresAt;
        this.status = status;
        this.updatedAt = Instant.now();
    }
}
