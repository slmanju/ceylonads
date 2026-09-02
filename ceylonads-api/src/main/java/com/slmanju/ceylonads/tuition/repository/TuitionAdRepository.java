package com.slmanju.ceylonads.tuition.repository;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

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

    // Detail path: category + seller, in one round trip.
    @EntityGraph(attributePaths = {"category", "seller"})
    Optional<Ad> findByIdAndStatusAndSourceChannel(Long id, AdStatus status, SourceChannel sourceChannel);

    // Similar-classes candidate pool: active TUITION ads in the same leaf category, excluding the
    // current ad, capped via findTop20 so no LIMIT/count handling or generic search pipeline is
    // needed.
    @EntityGraph(attributePaths = {"category", "seller"})
    List<Ad> findTop20ByCategoryIdAndStatusAndSourceChannelAndIdNotOrderByCreatedAtDesc(
            Long categoryId, AdStatus status, SourceChannel sourceChannel, Long excludeId);

    // Homepage/list "Latest Classes" feed: active TUITION ads, newest first, across every Tuition
    // category (no category-tree join needed now that sourceChannel alone identifies them).
    // Spring Data derives the COUNT query automatically for this Page<> return type, which is
    // required here (unlike the featured carousel) since this is genuine page-by-page browsing
    // that needs a real totalPages.
    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Ad> findByStatusAndSourceChannelOrderByCreatedAtDesc(AdStatus status, SourceChannel sourceChannel, Pageable pageable);

    // Tuition-owned "My Classes": the seller's own listings, restricted to TUITION so a seller's
    // MAIN_SITE/BOARDING ads never leak into this view.
    @EntityGraph(attributePaths = {"category", "seller"})
    List<Ad> findBySellerIdAndSourceChannelOrderByCreatedAtDesc(Long sellerId, SourceChannel sourceChannel);
}
