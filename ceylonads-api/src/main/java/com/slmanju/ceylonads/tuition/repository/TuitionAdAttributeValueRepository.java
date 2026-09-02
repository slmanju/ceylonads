package com.slmanju.ceylonads.tuition.repository;

import com.slmanju.ceylonads.ad.entity.AdAttributeValue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

// Read-only, scoped to the small fixed set of attribute keys the tuition views actually render.
// Deliberately does not reuse AdAttributeValueRepository.findDetailedByAdId, which left-join-fetches
// every option for every selected definition - labels here are resolved separately via a small
// batched IN query (see TuitionClassService), so this query never touches attribute_options.
public interface TuitionAdAttributeValueRepository extends Repository<AdAttributeValue, Long> {

    @EntityGraph(attributePaths = "attributeDefinition")
    @Query("""
            select av from AdAttributeValue av
            join av.attributeDefinition d
            where av.ad.id in :adIds and d.key in :keys
            order by av.ad.id asc, d.displayOrder asc, av.id asc
            """)
    List<AdAttributeValue> findByAdIdInAndKeyIn(@Param("adIds") Collection<Long> adIds, @Param("keys") Collection<String> keys);
}
