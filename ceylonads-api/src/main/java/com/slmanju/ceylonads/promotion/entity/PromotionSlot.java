package com.slmanju.ceylonads.promotion.entity;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.category.entity.Category;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * The physical, sellable placement inventory a {@link PromotionPlan} sells access to. A
 * {@code PlacementType} describes where on the site a promotion shows (ranking/display concern);
 * a {@code PromotionSlot} is the concrete, capacity-limited pool of that placement (e.g. the
 * homepage Featured section holds 8 concurrent ads) that {@code Promotion} rows compete for.
 */
@Entity
@Table(name = "promotion_slots", uniqueConstraints = @UniqueConstraint(name = "uk_promotion_slot_code", columnNames = "code"))
public class PromotionSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    // Immutable after creation, same reasoning as PromotionPlan.placementType: changing where a
    // slot's inventory shows would retroactively change the meaning of promotions already sold
    // against it.
    @Enumerated(EnumType.STRING)
    @Column(name = "placement_type", nullable = false, length = 30, updatable = false)
    private PlacementType placementType;

    // Immutable after creation for the same reason. Null for placements that aren't
    // category-bound (HOME_FEATURED, HOME_BANNER, TOP_SEARCH).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", updatable = false)
    private Category category;

    // Which storefront/vertical this slot's inventory belongs to (see ads.source_channel).
    // Immutable after creation for the same reason as placementType/category: it's what keeps a
    // channel's promotion catalog (e.g. compatiblePlansForTuitionAd) from leaking another
    // channel's generic placements, so it must not change out from under promotions already sold.
    @Enumerated(EnumType.STRING)
    @Column(name = "source_channel", nullable = false, length = 20, updatable = false)
    private SourceChannel sourceChannel;

    // Maximum number of ACTIVE/SCHEDULED campaigns allowed to be sold for this slot. Distinct from
    // visibleCount: capacity is sellable inventory, visibleCount is how many render to a visitor
    // at once (see the class javadoc and visibleCount itself).
    @Column(nullable = false)
    private int capacity;

    // Maximum number of campaigns rendered to a visitor at one time (e.g. a carousel page size).
    // Always <= capacity; capacity/overlap availability math must keep using capacity, never this.
    @Column(name = "visible_count", nullable = false)
    private int visibleCount;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PromotionSlot() {
    }

    public PromotionSlot(
            String code,
            String name,
            String description,
            PlacementType placementType,
            Category category,
            SourceChannel sourceChannel,
            int capacity,
            int visibleCount,
            int displayOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.placementType = placementType;
        this.category = category;
        this.sourceChannel = sourceChannel;
        this.capacity = capacity;
        this.visibleCount = visibleCount;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public PlacementType getPlacementType() { return placementType; }
    public Category getCategory() { return category; }
    public SourceChannel getSourceChannel() { return sourceChannel; }
    public int getCapacity() { return capacity; }
    public int getVisibleCount() { return visibleCount; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String name, String description, int capacity, int visibleCount, int displayOrder) {
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.visibleCount = visibleCount;
        this.displayOrder = displayOrder;
        this.updatedAt = Instant.now();
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }
}
