package com.slmanju.ceylonads.location.repository;

import com.slmanju.ceylonads.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findBySlugAndActiveTrue(String slug);
    Optional<Location> findBySlug(String slug);
    List<Location> findAllByActiveTrueOrderByNameAsc();
}
