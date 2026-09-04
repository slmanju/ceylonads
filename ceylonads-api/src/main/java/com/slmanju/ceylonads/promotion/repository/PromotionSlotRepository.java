package com.slmanju.ceylonads.promotion.repository;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.promotion.entity.PlacementType;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PromotionSlotRepository extends JpaRepository<PromotionSlot, Long> {
    Optional<PromotionSlot> findByCode(String code);

    // Batched lookup for callers resolving several known slot codes in one request (e.g. the
    // Tuition search page's grouped promotion read) instead of one findByCode per slot.
    List<PromotionSlot> findByCodeIn(Collection<String> codes);

    // PromotionSlotMapper touches category for every slot; fetch it here instead of lazily per
    // slot when mapping a list to PromotionSlotResponse.
    @EntityGraph(attributePaths = "category")
    List<PromotionSlot> findAllByOrderByDisplayOrderAscIdAsc();

    @EntityGraph(attributePaths = "category")
    List<PromotionSlot> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    // Used to resolve the specific category-bound slot (e.g. VEHICLES_FEATURED) a category page
    // should read visibleCount/capacity from, walking up from the page's own category.
    Optional<PromotionSlot> findByPlacementTypeAndCategory(PlacementType placementType, Category category);

    // Used to resolve the single well-known slot for a non-category-scoped placement (e.g.
    // SEARCH_TOP for TOP_SEARCH) without hard-coding its slot code.
    List<PromotionSlot> findByPlacementTypeAndCategoryIsNull(PlacementType placementType);

    // Tuition admin console's read-only slot picker (feeds the Promotion Plan create form) - no
    // Tuition slot CRUD UI exists, only this list.
    @EntityGraph(attributePaths = "category")
    List<PromotionSlot> findBySourceChannelOrderByDisplayOrderAscIdAsc(SourceChannel sourceChannel);
}
