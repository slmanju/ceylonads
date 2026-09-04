package com.slmanju.ceylonads.tuition.repository;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Read-only view onto the shared `ads` table for the tuition vertical. Extends the bare Repository
// marker (not JpaRepository) so this domain can only ever read Ad rows - writes to ads stay
// exclusively in the `ad` domain's own AdRepository/AdService.
//
// Every query here filters by sourceChannel = TUITION, not by category: source_channel is now the
// authoritative "does this ad belong to the Tuition vertical" signal (only TuitionClassService's
// own create/update ever assigns it), so it replaces the category-tree walk these queries used
// before source_channel existed. Category is still validated once, at create/update time (see
// TuitionClassService.requireTuitionCategory) - it just isn't re-checked on every read anymore.
public interface TuitionAdRepository extends Repository<Ad, Long> {

    // Detail path: category + seller, in one round trip. Expiry-aware: an ACTIVE Tuition ad past
    // its expiresAt must 404 here immediately, not just after the next scheduler sweep - see
    // AdSpecifications.notExpired for the equivalent guard on the generic search path.
    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("select a from Ad a where a.id = :id and a.status = :status and a.sourceChannel = :sourceChannel "
            + "and (a.expiresAt is null or a.expiresAt > :now)")
    Optional<Ad> findPublicByIdAndStatusAndSourceChannel(
            @Param("id") Long id, @Param("status") AdStatus status, @Param("sourceChannel") SourceChannel sourceChannel,
            @Param("now") Instant now);

    // Similar-classes candidate pool: active, unexpired TUITION ads in the same leaf category,
    // excluding the current ad, capped via the Pageable so no separate COUNT query is needed.
    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("select a from Ad a where a.category.id = :categoryId and a.status = :status and a.sourceChannel = :sourceChannel "
            + "and a.id <> :excludeId and (a.expiresAt is null or a.expiresAt > :now) order by a.createdAt desc")
    List<Ad> findSimilarActive(
            @Param("categoryId") Long categoryId, @Param("status") AdStatus status, @Param("sourceChannel") SourceChannel sourceChannel,
            @Param("excludeId") Long excludeId, @Param("now") Instant now, Pageable pageable);

    // Homepage/list "Latest Classes" feed: active, unexpired TUITION ads, newest first, across
    // every Tuition category (no category-tree join needed now that sourceChannel alone identifies
    // them). An explicit countQuery is required alongside the content query (unlike the featured
    // carousel) since this is genuine page-by-page browsing that needs a real totalPages.
    @EntityGraph(attributePaths = {"category", "seller"})
    @Query(value = "select a from Ad a where a.status = :status and a.sourceChannel = :sourceChannel "
            + "and (a.expiresAt is null or a.expiresAt > :now) order by a.createdAt desc",
            countQuery = "select count(a) from Ad a where a.status = :status and a.sourceChannel = :sourceChannel "
                    + "and (a.expiresAt is null or a.expiresAt > :now)")
    Page<Ad> findActiveLatest(@Param("status") AdStatus status, @Param("sourceChannel") SourceChannel sourceChannel,
            @Param("now") Instant now, Pageable pageable);

    // Tuition-owned "My Classes": the seller's own listings, restricted to TUITION so a seller's
    // MAIN_SITE/BOARDING ads never leak into this view. Deliberately not expiry-filtered - an
    // owner must keep seeing their own EXPIRED listings in My Classes history.
    @EntityGraph(attributePaths = {"category", "seller"})
    List<Ad> findBySellerIdAndSourceChannelOrderByCreatedAtDesc(Long sellerId, SourceChannel sourceChannel);

    // The 15-concurrent-listing cap (see TuitionClassService.create): counts only statuses that
    // currently consume a seller's Tuition inventory (PENDING_REVIEW, ACTIVE) - REJECTED/EXPIRED/
    // DEACTIVATED never count, regardless of how many a seller has accumulated historically.
    long countBySellerIdAndSourceChannelAndStatusIn(Long sellerId, SourceChannel sourceChannel, Collection<AdStatus> statuses);
}
