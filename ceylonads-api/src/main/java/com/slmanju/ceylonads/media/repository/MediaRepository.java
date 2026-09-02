package com.slmanju.ceylonads.media.repository;

import com.slmanju.ceylonads.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByAdIdOrderByDisplayOrderAscIdAsc(Long adId);

    // Batch path for lists of ads: one query total regardless of how many ads are passed in,
    // instead of one media query per ad.
    List<Media> findByAdIdInOrderByAdIdAscDisplayOrderAscIdAsc(Collection<Long> adIds);

    long countByAdId(Long adId);
}
