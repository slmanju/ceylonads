package com.slmanju.ceylonads.promotion.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotion_plans", uniqueConstraints = @UniqueConstraint(name = "uk_promotion_plan_code", columnNames = "code"))
public class PromotionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    // Immutable after creation: changing which slot a plan sells would retroactively change the
    // meaning of promotions already sold under this code. The slot itself carries placement type,
    // category binding, and capacity - a plan never duplicates that configuration.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_slot_id", nullable = false, updatable = false)
    private PromotionSlot slot;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    // When false, no Payment is created for a promotion sold under this plan - see
    // PromotionService for how these two flags determine a promotion's initial status.
    @Column(name = "payment_required", nullable = false)
    private boolean paymentRequired;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PromotionPlan() {
    }

    public PromotionPlan(
            String code,
            String name,
            String description,
            PromotionSlot slot,
            int durationDays,
            BigDecimal price,
            boolean paymentRequired,
            boolean approvalRequired,
            int displayOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.slot = slot;
        this.durationDays = durationDays;
        this.price = price;
        this.paymentRequired = paymentRequired;
        this.approvalRequired = approvalRequired;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public PromotionSlot getSlot() { return slot; }
    public PlacementType getPlacementType() { return slot.getPlacementType(); }
    public int getDurationDays() { return durationDays; }
    public BigDecimal getPrice() { return price; }
    public boolean isActive() { return active; }
    public boolean isPaymentRequired() { return paymentRequired; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public int getDisplayOrder() { return displayOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(
            String name,
            String description,
            BigDecimal price,
            int durationDays,
            boolean paymentRequired,
            boolean approvalRequired,
            int displayOrder) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationDays = durationDays;
        this.paymentRequired = paymentRequired;
        this.approvalRequired = approvalRequired;
        this.displayOrder = displayOrder;
        this.updatedAt = Instant.now();
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }
}
