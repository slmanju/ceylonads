package com.slmanju.ceylonads.tuition.repository;

import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

// Read-only view onto the shared `promotions` table for the Tuition UI's Featured Tuition
// carousel and search-page sidebar promotions. Extends the bare Repository marker (not
// JpaRepository), same as TuitionAdRepository - this domain only ever reads Promotion rows;
// writes stay exclusively in the `promotion` domain's own PromotionRepository/PromotionService.
public interface TuitionPromotionRepository extends Repository<Promotion, Long> {

    // Active, currently-live promotions in the given slot whose ad is still ACTIVE, unexpired, and
    // TUITION - a promotion can outlive its ad going inactive/sold/expired without being
    // cancelled, and the featured carousel must never surface those; the sourceChannel check is
    // what actually guarantees a MAIN_SITE/BOARDING ad can never surface here just because it
    // happens to hold a promotion in a category-bound slot the Tuition UI reads (the slot's own
    // category binding is a promotion-config concern, not an ad filter). Capped via Pageable so
    // this fixed-size carousel never needs a COUNT query.
    @EntityGraph(attributePaths = {"ad", "ad.category", "ad.seller"})
    @Query("select p from Promotion p where p.status = :status and p.plan.slot = :slot and p.ad.status = :adStatus "
            + "and p.ad.sourceChannel = :sourceChannel and p.endsAt > :now "
            + "and (p.ad.expiresAt is null or p.ad.expiresAt > :now) order by p.startsAt desc, p.id asc")
    List<Promotion> findByStatusAndPlan_SlotAndAd_StatusAndAd_SourceChannelAndEndsAtAfterOrderByStartsAtDescIdAsc(
            @Param("status") PromotionStatus status, @Param("slot") PromotionSlot slot, @Param("adStatus") AdStatus adStatus,
            @Param("sourceChannel") SourceChannel sourceChannel, @Param("now") Instant now, Pageable pageable);

    // Same eligibility rules as above, but across several slots in one query - used by the search
    // page's sidebar (top/middle/bottom are 3 distinct slots) so it never issues one query per
    // slot. Each slot's own capacity/visibleCount already bounds how many rows can ever be ACTIVE
    // for it at once, so no Pageable/COUNT is needed here.
    @EntityGraph(attributePaths = {"ad", "ad.category", "ad.seller"})
    @Query("select p from Promotion p where p.status = :status and p.plan.slot in :slots and p.ad.status = :adStatus "
            + "and p.ad.sourceChannel = :sourceChannel and p.endsAt > :now "
            + "and (p.ad.expiresAt is null or p.ad.expiresAt > :now) order by p.startsAt desc, p.id asc")
    List<Promotion> findByStatusAndPlan_SlotInAndAd_StatusAndAd_SourceChannelAndEndsAtAfterOrderByStartsAtDescIdAsc(
            @Param("status") PromotionStatus status, @Param("slots") Collection<PromotionSlot> slots, @Param("adStatus") AdStatus adStatus,
            @Param("sourceChannel") SourceChannel sourceChannel, @Param("now") Instant now);
}
