package com.slmanju.ceylonads.promotion.entity;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A temporary, channel-scoped price override for one or more {@link PromotionPlan}s (e.g. a
 * launch offer or a seasonal discount). Never mutates {@code PromotionPlan.price} - the base
 * price stays the plan's permanent value, and {@link PromotionPricingService} resolves the
 * currently-effective price by finding the active campaign (if any) covering a plan at a given
 * instant.
 */
@Entity
@Table(name = "promotion_campaigns", uniqueConstraints = @UniqueConstraint(name = "uk_promotion_campaign_code", columnNames = "code"))
public class PromotionCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Immutable after creation: renaming a campaign's code would break admin tooling and audit
    // trails referencing it by code (same reasoning as PromotionPlan.code).
    @Column(nullable = false, length = 60, updatable = false)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    // Immutable after creation, same reasoning as PromotionSlot.sourceChannel: which storefront a
    // campaign applies to shouldn't change after plans have been mapped to it.
    @Enumerated(EnumType.STRING)
    @Column(name = "source_channel", nullable = false, length = 20, updatable = false)
    private SourceChannel sourceChannel;

    // Immutable after creation: switching a campaign between FIXED_PRICE and
    // PERCENTAGE_DISCOUNT after the fact would silently change what fixedPrice/discountPercent
    // mean without a corresponding data migration.
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 20, updatable = false)
    private PricingType pricingType;

    // Populated only when pricingType == PERCENTAGE_DISCOUNT.
    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    // Populated only when pricingType == FIXED_PRICE.
    @Column(name = "fixed_price", precision = 19, scale = 2)
    private BigDecimal fixedPrice;

    // Optional floor applied after a PERCENTAGE_DISCOUNT calculation, so a cheap plan's discount
    // never undercuts a more important campaign price (e.g. a launch offer). Ignored for
    // FIXED_PRICE campaigns.
    @Column(name = "minimum_price", precision = 19, scale = 2)
    private BigDecimal minimumPrice;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    // Storefront presentation, all plain text (no HTML) - see PromotionCampaignService for the
    // "customer_visible requires these non-blank" invariant. Null/blank is fine for a campaign
    // that's purely a pricing override with no customer-facing presence.
    @Column(length = 180)
    private String headline;

    @Column(length = 500)
    private String message;

    @Column(name = "cta_label", length = 80)
    private String ctaLabel;

    // Whether this campaign may be exposed through the public storefront campaign API at all -
    // see PromotionCampaignService#findActiveCustomerCampaign.
    @Column(name = "customer_visible", nullable = false)
    private boolean customerVisible = false;

    @Column(name = "show_banner", nullable = false)
    private boolean showBanner = false;

    @Column(name = "show_modal", nullable = false)
    private boolean showModal = false;

    @ManyToMany
    @JoinTable(
            name = "promotion_campaign_plans",
            joinColumns = @JoinColumn(name = "campaign_id"),
            inverseJoinColumns = @JoinColumn(name = "promotion_plan_id"))
    private Set<PromotionPlan> plans = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PromotionCampaign() {
    }

    public PromotionCampaign(
            String code,
            String name,
            String description,
            SourceChannel sourceChannel,
            PricingType pricingType,
            BigDecimal discountPercent,
            BigDecimal fixedPrice,
            BigDecimal minimumPrice,
            Instant startsAt,
            Instant endsAt,
            Set<PromotionPlan> plans,
            String headline,
            String message,
            String ctaLabel,
            boolean customerVisible,
            boolean showBanner,
            boolean showModal) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.sourceChannel = sourceChannel;
        this.pricingType = pricingType;
        this.discountPercent = discountPercent;
        this.fixedPrice = fixedPrice;
        this.minimumPrice = minimumPrice;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.plans = plans;
        this.headline = headline;
        this.message = message;
        this.ctaLabel = ctaLabel;
        this.customerVisible = customerVisible;
        this.showBanner = showBanner;
        this.showModal = showModal;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public SourceChannel getSourceChannel() { return sourceChannel; }
    public PricingType getPricingType() { return pricingType; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public BigDecimal getFixedPrice() { return fixedPrice; }
    public BigDecimal getMinimumPrice() { return minimumPrice; }
    public boolean isActive() { return active; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Set<PromotionPlan> getPlans() { return plans; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getHeadline() { return headline; }
    public String getMessage() { return message; }
    public String getCtaLabel() { return ctaLabel; }
    public boolean isCustomerVisible() { return customerVisible; }
    public boolean isShowBanner() { return showBanner; }
    public boolean isShowModal() { return showModal; }

    public void update(
            String name,
            String description,
            BigDecimal discountPercent,
            BigDecimal fixedPrice,
            BigDecimal minimumPrice,
            Instant startsAt,
            Instant endsAt,
            Set<PromotionPlan> plans,
            String headline,
            String message,
            String ctaLabel,
            boolean customerVisible,
            boolean showBanner,
            boolean showModal) {
        this.name = name;
        this.description = description;
        this.discountPercent = discountPercent;
        this.fixedPrice = fixedPrice;
        this.minimumPrice = minimumPrice;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.plans = plans;
        this.headline = headline;
        this.message = message;
        this.ctaLabel = ctaLabel;
        this.customerVisible = customerVisible;
        this.showBanner = showBanner;
        this.showModal = showModal;
        this.updatedAt = Instant.now();
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }

    /**
     * DEV-only convenience for SampleDataSeeder: re-windows an already-configured campaign to be
     * active "now" without touching pricing, name, description, or plans - unlike {@link #update},
     * which requires re-supplying every field and is meant for the real admin edit flow. Callers
     * are responsible for the storefront invariants PromotionCampaignService normally validates
     * (customer_visible/non-blank presentation fields, show_banner/show_modal requiring
     * customer_visible) since this bypasses that service entirely.
     */
    public void activateForDevStorefront(Instant startsAt, Instant endsAt) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.active = true;
        this.updatedAt = Instant.now();
    }
}
