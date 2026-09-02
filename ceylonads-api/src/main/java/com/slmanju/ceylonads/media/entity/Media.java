package com.slmanju.ceylonads.media.entity;

import com.slmanju.ceylonads.ad.entity.Ad;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "media", indexes = {
        @Index(name = "idx_media_ad_display", columnList = "ad_id,display_order")
})
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable: only set when ownerType is AD. Payment receipts have no ad.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id")
    private Ad ad;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 30)
    private MediaOwnerType ownerType;

    @Column(nullable = false, length = 200)
    private String storageKey;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Media() {
    }

    public Media(Ad ad, String storageKey, String contentType, int displayOrder) {
        this.ad = ad;
        this.ownerType = MediaOwnerType.AD;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.displayOrder = displayOrder;
    }

    public Media(String storageKey, String contentType) {
        this.ownerType = MediaOwnerType.PAYMENT_RECEIPT;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.displayOrder = 0;
    }

    public static Media forPromotionBanner(String storageKey, String contentType) {
        Media media = new Media();
        media.ownerType = MediaOwnerType.PROMOTION_BANNER;
        media.storageKey = storageKey;
        media.contentType = contentType;
        media.displayOrder = 0;
        return media;
    }

    public Long getId() { return id; }
    public Ad getAd() { return ad; }
    public MediaOwnerType getOwnerType() { return ownerType; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public int getDisplayOrder() { return displayOrder; }
    public Instant getCreatedAt() { return createdAt; }
}
