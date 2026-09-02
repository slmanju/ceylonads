package com.slmanju.ceylonads.ad.repository;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdRepository extends JpaRepository<Ad, Long>, JpaSpecificationExecutor<Ad> {

    // Detail read path: category/seller in the same round trip as the ad itself, instead of two
    // separate lazy loads once the response mapper touches each association. Locations are a
    // collection (0..N) so they're batch-loaded separately via AdLocationService, the same way
    // media/attributes are, rather than joined into this entity graph.
    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("select a from Ad a where a.id = :id and a.status = :status")
    Optional<Ad> findDetailByIdAndStatus(@Param("id") Long id, @Param("status") AdStatus status);

    // MAIN public detail lookup: same shape as findDetailByIdAndStatus, but also scoped to
    // MAIN_SITE so a TUITION/BOARDING ad can't be reached through the main storefront's public
    // detail endpoint just by knowing its id. Internal/admin lookups keep using
    // findDetailByIdAndStatus/findDetailById, which stay channel-agnostic.
    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("select a from Ad a where a.id = :id and a.status = :status and a.sourceChannel = :sourceChannel")
    Optional<Ad> findDetailByIdAndStatusAndSourceChannel(
            @Param("id") Long id, @Param("status") AdStatus status, @Param("sourceChannel") SourceChannel sourceChannel);

    // Same fetch shape as findDetailByIdAndStatus, for admin/ownership flows that operate on an ad
    // regardless of its current status.
    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("select a from Ad a where a.id = :id")
    Optional<Ad> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category", "seller"})
    List<Ad> findByStatusOrderByCreatedAtAsc(AdStatus status);

    // MAIN moderator queue: same as findByStatusOrderByCreatedAtAsc, restricted to MAIN_SITE. The
    // unrestricted method above stays in use for ADMIN, which moderates every channel.
    @EntityGraph(attributePaths = {"category", "seller"})
    List<Ad> findByStatusAndSourceChannelOrderByCreatedAtAsc(AdStatus status, SourceChannel sourceChannel);

    // DEV-only: lets SampleDataSeeder reconcile the curated Tuition sample set on every seed run -
    // any existing TUITION-channel ad whose title isn't in the current curated list is stale
    // (e.g. left over from a superseded sample list or a since-removed generator) and gets cleaned
    // up before the current list is (re-)applied.
    List<Ad> findBySourceChannel(SourceChannel sourceChannel);

    @EntityGraph(attributePaths = {"category", "seller"})
    List<Ad> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @EntityGraph(attributePaths = {"category", "seller"})
    List<Ad> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, AdStatus status);

    // Capped and ordered on the (status, created_at) index that already backs the public search
    // query, so the sitemap listing doesn't need a dedicated index. Only id/title/updatedAt are
    // read from these, so no entity graph is needed here.
    List<Ad> findTop2000ByStatusOrderByCreatedAtDesc(AdStatus status);

    // Same, restricted to MAIN_SITE: the sitemap only ever links /ads/{slug} pages, which is now a
    // MAIN-storefront-only public route (see findDetailByIdAndStatusAndSourceChannel) - listing a
    // TUITION/BOARDING ad here would publish a dead link.
    List<Ad> findTop2000ByStatusAndSourceChannelOrderByCreatedAtDesc(AdStatus status, SourceChannel sourceChannel);

    // Applies to every specification-based findAll (currently just the promoted-ads pool in
    // AdSearchService): the promoted subset is small, but each result still needs
    // category/seller for its AdResponse, so fetch them in the same query rather than lazily per ad.
    @EntityGraph(attributePaths = {"category", "seller"})
    @Override
    List<Ad> findAll(Specification<Ad> spec);
}
