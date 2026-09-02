package com.slmanju.ceylonads.ad.repository;

import com.slmanju.ceylonads.ad.entity.AdLocation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AdLocationRepository extends JpaRepository<AdLocation, Long> {

    // Single-ad detail path.
    @EntityGraph(attributePaths = "location")
    List<AdLocation> findByAdIdOrderByLocationNameAsc(Long adId);

    // Batch path for lists of ads, same reasoning as AdAttributeValueRepository's batch method:
    // one query total regardless of list size, grouped by ad id in the caller.
    @EntityGraph(attributePaths = "location")
    @Query("select al from AdLocation al where al.ad.id in :adIds order by al.ad.id asc, al.location.name asc")
    List<AdLocation> findByAdIdInOrderByAdIdAsc(@Param("adIds") Collection<Long> adIds);

    @Modifying
    @Query("delete from AdLocation al where al.ad.id = :adId")
    void deleteByAdId(@Param("adId") Long adId);
}
