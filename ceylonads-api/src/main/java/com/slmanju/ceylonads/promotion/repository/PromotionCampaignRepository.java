package com.slmanju.ceylonads.promotion.repository;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.promotion.entity.PromotionCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaign, Long> {

    Optional<PromotionCampaign> findByCode(String code);

    List<PromotionCampaign> findAllByOrderByIdAsc();

    // Small, admin-curated set per channel (same reasoning as findActiveFor below) - used both to
    // resolve the live storefront campaign and to detect overlapping storefront campaigns at
    // configuration time (see PromotionCampaignService#requireNoOverlappingStorefrontCampaign).
    List<PromotionCampaign> findBySourceChannel(SourceChannel sourceChannel);

    // The plan currently being priced may have more than one campaign configured against it in
    // theory (e.g. an admin overlaps a launch and a seasonal offer by mistake); the caller takes
    // the first match rather than this query enforcing exclusivity, since a DB-level "at most one
    // active campaign per plan at a time" constraint would be awkward to express and isn't worth
    // it for what is, in practice, an admin-curated and small set of campaigns.
    @Query("select c from PromotionCampaign c join c.plans p "
            + "where c.active = true and c.sourceChannel = :channel and p.id = :planId "
            + "and :now between c.startsAt and c.endsAt")
    List<PromotionCampaign> findActiveFor(
            @Param("channel") SourceChannel channel, @Param("planId") Long planId, @Param("now") Instant now);
}
