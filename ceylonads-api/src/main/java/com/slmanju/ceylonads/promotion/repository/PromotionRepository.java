package com.slmanju.ceylonads.promotion.repository;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.promotion.entity.PlacementType;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.entity.PromotionSlot;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    // Every method below annotated with an @EntityGraph of {"ad", "customer", "plan", "plan.slot",
    // "bannerMedia"} feeds PromotionMapper, which touches exactly those associations - fetching
    // them here avoids 5 lazy loads per promotion when mapping a list to PromotionResponse.
    @EntityGraph(attributePaths = {"ad", "customer", "plan", "plan.slot", "bannerMedia"})
    List<Promotion> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Tuition UI's GET /api/tuition/promotions/my: same as findByCustomerIdOrderByCreatedAtDesc but
    // channel-scoped, so a tutor who also sells on MAIN_SITE/BOARDING under the same account never
    // sees those promotions mixed into their Tuition list. The Ad_SourceChannel path implies an
    // inner join, which also naturally excludes banner promotions (no ad) - correct here, since
    // banner promotions aren't something a tutor buys through this self-service flow.
    @EntityGraph(attributePaths = {"ad", "customer", "plan", "plan.slot", "bannerMedia"})
    List<Promotion> findByCustomerIdAndAd_SourceChannelOrderByCreatedAtDesc(Long customerId, SourceChannel sourceChannel);

    @EntityGraph(attributePaths = {"ad", "customer", "plan", "plan.slot", "bannerMedia"})
    List<Promotion> findByStatusOrderByCreatedAtDesc(PromotionStatus status);

    @EntityGraph(attributePaths = {"ad", "customer", "plan", "plan.slot", "bannerMedia"})
    List<Promotion> findAllByOrderByCreatedAtDesc();

    // Tuition admin console's channel-scoped equivalents of the two methods above. Scoped via
    // plan.slot.sourceChannel (not ad.sourceChannel): every Promotion has a non-null plan->slot,
    // but ad is null for BANNER_PROMOTION, so this is the only path that works for both kinds.
    @EntityGraph(attributePaths = {"ad", "customer", "plan", "plan.slot", "bannerMedia"})
    List<Promotion> findByPlan_Slot_SourceChannelOrderByCreatedAtDesc(SourceChannel sourceChannel);

    @EntityGraph(attributePaths = {"ad", "customer", "plan", "plan.slot", "bannerMedia"})
    List<Promotion> findByStatusAndPlan_Slot_SourceChannelOrderByCreatedAtDesc(PromotionStatus status, SourceChannel sourceChannel);

    // Tuition admin dashboard summary counts.
    long countByStatusAndPlan_Slot_SourceChannel(PromotionStatus status, SourceChannel sourceChannel);

    long countByStatusInAndPlan_Slot_SourceChannel(Collection<PromotionStatus> statuses, SourceChannel sourceChannel);

    boolean existsByAdIdAndPlan_SlotAndStatusIn(Long adId, PromotionSlot slot, Collection<PromotionStatus> statuses);

    // DEV-only: lets SampleDataSeeder find promotions on ads it's about to clean up (stale Tuition
    // sample ads) so they can be deleted before the ad itself is removed.
    List<Promotion> findByAdIdIn(Collection<Long> adIds);

    // Tuition's deactivation guard (see PromotionService.activePromotionEndsAt /
    // TuitionClassService.deactivateOwned): the latest-ending currently-active promotion on this
    // ad, if any - used both to decide whether to block deactivation and to show the tutor the
    // date it becomes possible again.
    Optional<Promotion> findTopByAdIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(Long adId, PromotionStatus status, Instant now);

    // ad.location was removed - an ad now has 0..N locations, batch-loaded separately by
    // PromotionService via AdLocationService rather than joined into this entity graph.
    @EntityGraph(attributePaths = {"ad", "ad.category", "ad.seller"})
    List<Promotion> findByStatusAndPlan_Slot_PlacementTypeAndEndsAtAfterOrderByStartsAtDescIdAsc(
            PromotionStatus status, PlacementType placementType, Instant now, Pageable pageable);

    // MAIN-storefront-only variant of the above: used by PromotionService.homeFeaturedAds, whose
    // carousel belongs specifically to the main CeylonAds homepage, so a TUITION/BOARDING ad must
    // not surface there just because it holds a generic HOME_FEATURED promotion.
    @EntityGraph(attributePaths = {"ad", "ad.category", "ad.seller"})
    List<Promotion> findByStatusAndPlan_Slot_PlacementTypeAndEndsAtAfterAndAd_SourceChannelOrderByStartsAtDescIdAsc(
            PromotionStatus status, PlacementType placementType, Instant now, SourceChannel sourceChannel, Pageable pageable);

    // Only promotion.getAd().getId() is read from these (see AdSearchService/PromotionService
    // ranking code): the ad's id is available on the lazy proxy without a further query, so no
    // entity graph is needed here.
    List<Promotion> findByAdIdInAndStatusAndPlan_Slot_PlacementTypeAndEndsAtAfter(
            Collection<Long> adIds, PromotionStatus status, PlacementType placementType, Instant now);

    // Exact-slot-code sibling of the above, for ranking by a specific slot (e.g. TUITION_SEARCH_BOOST)
    // rather than a whole placement type - see AdSearchService's Tuition Search Boost overload.
    List<Promotion> findByAdIdInAndStatusAndPlan_Slot_CodeAndEndsAtAfter(
            Collection<Long> adIds, PromotionStatus status, String slotCode, Instant now);

    // Currently-active (unexpired) promotions occupying a slot right now.
    long countByPlan_SlotAndStatusAndEndsAtAfter(PromotionSlot slot, PromotionStatus status, Instant now);

    long countByPlan_SlotAndStatus(PromotionSlot slot, PromotionStatus status);

    @EntityGraph(attributePaths = {"ad", "customer", "plan", "plan.slot", "bannerMedia"})
    List<Promotion> findByPlan_SlotAndStatusOrderByStartsAtAscIdAsc(PromotionSlot slot, PromotionStatus status);

    // Only bannerMedia is read from these results (PromotionSlotService.activeBannersByCode maps
    // fields directly, not through PromotionMapper).
    @EntityGraph(attributePaths = "bannerMedia")
    List<Promotion> findByStatusAndPlan_SlotAndEndsAtAfterOrderByStartsAtAscIdAsc(
            PromotionStatus status, PromotionSlot slot, Instant now);

    // Used for slot-scoped public "featured" listings (e.g. category-featured carousels), bounded
    // by a Pageable so the result never exceeds the slot's capacity.
    // ad.location was removed - an ad now has 0..N locations, batch-loaded separately by
    // PromotionService via AdLocationService rather than joined into this entity graph.
    @EntityGraph(attributePaths = {"ad", "ad.category", "ad.seller"})
    List<Promotion> findByStatusAndPlan_SlotAndEndsAtAfterOrderByStartsAtDescIdAsc(
            PromotionStatus status, PromotionSlot slot, Instant now, Pageable pageable);

    // MAIN-storefront-only variant of the above: used by PromotionService.categoryFeaturedAds,
    // which backs /api/ads/category-featured - a main-site public endpoint (Tuition has its own
    // isolated category-featured query, TuitionFeaturedService, which never calls through here).
    @EntityGraph(attributePaths = {"ad", "ad.category", "ad.seller"})
    List<Promotion> findByStatusAndPlan_SlotAndEndsAtAfterAndAd_SourceChannelOrderByStartsAtDescIdAsc(
            PromotionStatus status, PromotionSlot slot, Instant now, SourceChannel sourceChannel, Pageable pageable);

    // General overlap check for a [start, end) window: a promotion's own [startsAt, endsAt) range
    // overlaps the window iff it starts before the window ends and ends after the window starts.
    // Only ACTIVE promotions carry real startsAt/endsAt values, so this naturally excludes
    // PENDING_PAYMENT promotions that haven't been scheduled into the slot yet. Run entirely in
    // the database - the promoted pool for a slot is expected to stay small, but this still
    // avoids ever loading promotion rows just to count them.
    @Query("select count(p) from Promotion p where p.plan.slot = :slot and p.status = com.slmanju.ceylonads.promotion.entity.PromotionStatus.ACTIVE "
            + "and p.startsAt < :end and p.endsAt > :start")
    long countOverlapping(@Param("slot") PromotionSlot slot, @Param("start") Instant start, @Param("end") Instant end);

    // A small, cheap normalization step: rather than run a scheduler, flip any promotion whose
    // end date has passed to EXPIRED whenever promotions are listed. Ranking queries never rely
    // on this - they always filter on endsAt independently - so a delayed flip never affects
    // what's shown publicly, only the status a customer/admin sees in their list.
    @Modifying
    @Query("update Promotion p set p.status = com.slmanju.ceylonads.promotion.entity.PromotionStatus.EXPIRED, p.updatedAt = :now "
            + "where p.status = com.slmanju.ceylonads.promotion.entity.PromotionStatus.ACTIVE and p.endsAt <= :now")
    int expireOverdue(@Param("now") Instant now);
}
