package com.slmanju.ceylonads.ad.repository;

import com.slmanju.ceylonads.ad.entity.AdAttributeValue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AdAttributeValueRepository extends JpaRepository<AdAttributeValue, Long> {

    // Single-ad detail path: definition and its options in one round trip. Safe to fetch every
    // option per row here since it's scoped to one ad's (small) attribute set - see
    // findByAdIdInOrderByDefinitionDisplayOrder below for why the batch path fetches options
    // separately instead.
    @Query("""
            select distinct av from AdAttributeValue av
            join fetch av.attributeDefinition d
            left join fetch d.options
            where av.ad.id = :adId
            order by d.displayOrder asc, av.id asc
            """)
    List<AdAttributeValue> findDetailedByAdId(@Param("adId") Long adId);

    // Batch path for lists of ads: only the definition is fetched here (not options), since
    // joining every option for every value across many ads would multiply rows a lot faster than
    // it does for a single ad. Callers fetch options separately, keyed by the small set of
    // distinct definition ids actually referenced.
    @EntityGraph(attributePaths = "attributeDefinition")
    @Query("select v from AdAttributeValue v where v.ad.id in :adIds order by v.ad.id asc, v.attributeDefinition.displayOrder asc, v.id asc")
    List<AdAttributeValue> findByAdIdInOrderByDefinitionDisplayOrder(@Param("adIds") Collection<Long> adIds);

    @Modifying
    @Query("delete from AdAttributeValue v where v.ad.id = :adId")
    void deleteByAdId(@Param("adId") Long adId);
}
