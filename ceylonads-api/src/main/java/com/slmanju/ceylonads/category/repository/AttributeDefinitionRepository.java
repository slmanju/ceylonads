package com.slmanju.ceylonads.category.repository;

import com.slmanju.ceylonads.category.entity.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {
    List<AttributeDefinition> findByCategoryIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long categoryId);
    List<AttributeDefinition> findByCategoryIdOrderByDisplayOrderAscIdAsc(Long categoryId);
    boolean existsByCategoryIdAndKey(Long categoryId, String key);
    Optional<AttributeDefinition> findByIdAndCategoryId(Long id, Long categoryId);

    // Filter-metadata endpoint: a category's own filterable attributes plus any inherited from its
    // ancestor chain, in one query.
    List<AttributeDefinition> findByCategoryIdInAndActiveTrueAndFilterableTrueOrderByDisplayOrderAscIdAsc(Collection<Long> categoryIds);

    // Search attribute-filter validation: look the requested keys up directly.
    List<AttributeDefinition> findByKeyInAndActiveTrueAndFilterableTrue(Collection<String> keys);

    // Tuition filter metadata: the small fixed set of tuition attribute keys, across every direct
    // child of the tuition root in one query (each child has its own definition per key - see
    // TuitionFilterMetadataService for why these aren't inherited from a single shared definition).
    List<AttributeDefinition> findByCategoryIdInAndKeyInAndActiveTrue(Collection<Long> categoryIds, Collection<String> keys);
}
