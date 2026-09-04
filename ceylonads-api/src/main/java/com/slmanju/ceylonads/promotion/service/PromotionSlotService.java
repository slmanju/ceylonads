package com.slmanju.ceylonads.promotion.service;

import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.media.storage.MediaStorage;
import com.slmanju.ceylonads.promotion.dto.PromotionBannerResponse;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotAdminRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotAvailabilityResponse;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotResponse;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotUpdateRequest;
import com.slmanju.ceylonads.promotion.dto.PromotionSlotUsageResponse;
import com.slmanju.ceylonads.promotion.entity.PlacementType;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import com.slmanju.ceylonads.promotion.mapper.PromotionMapper;
import com.slmanju.ceylonads.promotion.mapper.PromotionSlotMapper;
import com.slmanju.ceylonads.promotion.repository.PromotionRepository;
import com.slmanju.ceylonads.promotion.repository.PromotionSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Owns the physical, capacity-limited placement inventory ({@link PromotionSlot}) and the
 * overlap-based availability math for it. Deliberately not a booking engine: capacity is always
 * checked against a window that starts now (see {@link PromotionService#activate}), never an
 * arbitrary future reservation, so "available" answers stay simple and immediate.
 */
@Service
public class PromotionSlotService {

    private final PromotionSlotRepository slots;
    private final PromotionRepository promotions;
    private final CategoryRepository categories;
    private final PromotionSlotMapper mapper;
    private final PromotionMapper promotionMapper;
    private final MediaStorage storage;

    public PromotionSlotService(
            PromotionSlotRepository slots,
            PromotionRepository promotions,
            CategoryRepository categories,
            PromotionSlotMapper mapper,
            PromotionMapper promotionMapper,
            MediaStorage storage) {
        this.slots = slots;
        this.promotions = promotions;
        this.categories = categories;
        this.mapper = mapper;
        this.promotionMapper = promotionMapper;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<PromotionSlotResponse> allSlots() {
        return slots.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public PromotionSlotResponse create(PromotionSlotAdminRequest request) {
        if (slots.findByCode(request.code().trim()).isPresent()) {
            throw new BadRequestException("A promotion slot with this code already exists");
        }
        Category category = resolveCategory(request.placementType(), request.categorySlug());
        int visibleCount = request.visibleCount() == null ? 1 : request.visibleCount();
        requireVisibleCountWithinCapacity(visibleCount, request.capacity());
        PromotionSlot slot = slots.save(new PromotionSlot(
                request.code().trim(),
                request.name().trim(),
                request.description().trim(),
                request.placementType(),
                category,
                request.sourceChannel(),
                request.capacity(),
                visibleCount,
                request.displayOrder() == null ? 0 : request.displayOrder()));
        return mapper.toResponse(slot);
    }

    @Transactional
    public PromotionSlotResponse update(Long id, PromotionSlotUpdateRequest request) {
        PromotionSlot slot = requireSlot(id);
        int visibleCount = request.visibleCount() == null ? slot.getVisibleCount() : request.visibleCount();
        requireVisibleCountWithinCapacity(visibleCount, request.capacity());
        slot.update(
                request.name().trim(),
                request.description().trim(),
                request.capacity(),
                visibleCount,
                request.displayOrder() == null ? slot.getDisplayOrder() : request.displayOrder());
        slot.setActive(request.active());
        return mapper.toResponse(slot);
    }

    // visibleCount is how many campaigns render to a visitor at once; it can never exceed
    // capacity, the sellable inventory ceiling capacity/overlap availability math is based on.
    private void requireVisibleCountWithinCapacity(int visibleCount, int capacity) {
        if (visibleCount > capacity) {
            throw new BadRequestException("Visible at once cannot exceed capacity");
        }
    }

    @Transactional
    public PromotionSlotResponse setActive(Long id, boolean active) {
        PromotionSlot slot = requireSlot(id);
        slot.setActive(active);
        return mapper.toResponse(slot);
    }

    @Transactional(readOnly = true)
    public PromotionSlot requireSlot(Long id) {
        return slots.findById(id).orElseThrow(() -> new NotFoundException("Promotion slot not found"));
    }

    @Transactional(readOnly = true)
    public PromotionSlot requireSlotByCode(String code) {
        return slots.findByCode(code).orElseThrow(() -> new NotFoundException("Promotion slot not found"));
    }

    @Transactional(readOnly = true)
    public PromotionSlotAvailabilityResponse availability(Long id, LocalDate startDate, Integer durationDays) {
        PromotionSlot slot = requireSlot(id);
        Instant start = startDate != null ? startDate.atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.now();
        Instant end = start.plus(Duration.ofDays(durationDays != null ? durationDays : 1));
        return availabilityFor(slot, start, end);
    }

    // Shared by the public availability endpoint and the "compatible plans for my ad" listing,
    // where each plan's own duration defines the window checked.
    @Transactional(readOnly = true)
    public PromotionSlotAvailabilityResponse availabilityFor(PromotionSlot slot, Instant start, Instant end) {
        long overlapping = promotions.countOverlapping(slot, start, end);
        int remaining = (int) Math.max(0, slot.getCapacity() - overlapping);
        return new PromotionSlotAvailabilityResponse(slot.getId(), remaining > 0, slot.getCapacity(), remaining, start, end);
    }

    @Transactional(readOnly = true)
    public PromotionSlotUsageResponse usage(Long id) {
        PromotionSlot slot = requireSlot(id);
        Instant now = Instant.now();
        List<Promotion> active = promotions.findByPlan_SlotAndStatusOrderByStartsAtAscIdAsc(slot, PromotionStatus.ACTIVE)
                .stream().filter(p -> p.getEndsAt() != null && p.getEndsAt().isAfter(now)).toList();
        List<Promotion> pending = promotions.findByPlan_SlotAndStatusOrderByStartsAtAscIdAsc(slot, PromotionStatus.PENDING_PAYMENT);
        int remaining = Math.max(0, slot.getCapacity() - active.size());

        return new PromotionSlotUsageResponse(
                mapper.toResponse(slot),
                active.size(),
                pending.size(),
                remaining,
                active.stream().map(promotionMapper::toResponse).toList(),
                pending.stream().map(promotionMapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public List<PromotionBannerResponse> activeBannersByCode(String code) {
        PromotionSlot slot = requireSlotByCode(code);
        Instant now = Instant.now();
        return promotions.findByStatusAndPlan_SlotAndEndsAtAfterOrderByStartsAtAscIdAsc(PromotionStatus.ACTIVE, slot, now)
                .stream()
                .map(p -> new PromotionBannerResponse(
                        p.getId(),
                        p.getBannerMedia() != null ? storage.publicUrl(p.getBannerMedia().getStorageKey()) : null,
                        p.getTargetUrl(),
                        p.getStartsAt(),
                        p.getEndsAt()))
                .toList();
    }

    // Resolves the CATEGORY_FEATURED slot that governs a category page, walking up the category's
    // ancestor chain since a slot is typically sold against a parent grouping (e.g. "Vehicles")
    // while the page itself may be a leaf category (e.g. "Cars").
    @Transactional(readOnly = true)
    public Optional<PromotionSlot> resolveCategoryFeaturedSlot(String categorySlug) {
        if (categorySlug == null || categorySlug.isBlank()) {
            return Optional.empty();
        }
        return categories.findBySlug(categorySlug.trim()).flatMap(this::resolveCategoryFeaturedSlot);
    }

    private Optional<PromotionSlot> resolveCategoryFeaturedSlot(Category category) {
        Category current = category;
        while (current != null) {
            Optional<PromotionSlot> slot = slots.findByPlacementTypeAndCategory(PlacementType.CATEGORY_FEATURED, current);
            if (slot.isPresent()) {
                return slot;
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    // Resolves the single well-known slot for a non-category-scoped placement type (e.g.
    // TOP_SEARCH), without hard-coding a specific slot code.
    @Transactional(readOnly = true)
    public Optional<PromotionSlot> resolveSlotByPlacementType(PlacementType placementType) {
        return slots.findByPlacementTypeAndCategoryIsNull(placementType).stream().findFirst();
    }

    // Resolves a slot by its unique code, for callers ranking/reading a specific, exact placement
    // (e.g. Tuition's TUITION_SEARCH_BOOST) rather than the single well-known slot for a placement
    // type.
    @Transactional(readOnly = true)
    public Optional<PromotionSlot> resolveSlotByCode(String code) {
        return slots.findByCode(code);
    }

    private Category resolveCategory(PlacementType placementType, String categorySlug) {
        boolean categoryScoped = placementType.isCategoryScoped();
        boolean hasCategory = categorySlug != null && !categorySlug.isBlank();

        if (categoryScoped && !hasCategory) {
            throw new BadRequestException("A category is required for this placement type");
        }
        if (!categoryScoped && hasCategory) {
            throw new BadRequestException("This placement type cannot be bound to a category");
        }
        if (!hasCategory) {
            return null;
        }
        return categories.findBySlug(categorySlug.trim())
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }
}
