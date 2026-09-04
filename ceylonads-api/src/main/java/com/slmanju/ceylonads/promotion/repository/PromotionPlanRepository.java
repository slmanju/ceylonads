package com.slmanju.ceylonads.promotion.repository;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.promotion.entity.PromotionPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromotionPlanRepository extends JpaRepository<PromotionPlan, Long> {

    // PromotionPlanMapper touches slot and slot.category for every plan; fetch both here instead
    // of lazily per plan when mapping a list to PromotionPlanResponse.
    @EntityGraph(attributePaths = {"slot", "slot.category"})
    List<PromotionPlan> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    @EntityGraph(attributePaths = {"slot", "slot.category"})
    List<PromotionPlan> findAllByOrderByDisplayOrderAscIdAsc();

    Optional<PromotionPlan> findByCode(String code);

    // Tuition admin console's channel-scoped plan list - also backs the dashboard's
    // "Current Promotion Plans" count (filtered/counted in Java against TuitionPromotionCatalog,
    // since "current" isn't derivable from a single SQL predicate).
    @EntityGraph(attributePaths = {"slot", "slot.category"})
    List<PromotionPlan> findBySlot_SourceChannelOrderByDisplayOrderAscIdAsc(SourceChannel sourceChannel);
}
