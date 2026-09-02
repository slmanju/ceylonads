package com.slmanju.ceylonads.category.repository;

import com.slmanju.ceylonads.category.entity.AttributeOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttributeOptionRepository extends JpaRepository<AttributeOption, Long> {
    List<AttributeOption> findByAttributeDefinitionIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long attributeDefinitionId);
    List<AttributeOption> findByAttributeDefinitionIdOrderByDisplayOrderAscIdAsc(Long attributeDefinitionId);
    List<AttributeOption> findByAttributeDefinitionIdInAndActiveTrue(List<Long> attributeDefinitionIds);
    List<AttributeOption> findByAttributeDefinitionIdIn(List<Long> attributeDefinitionIds);
    boolean existsByAttributeDefinitionIdAndValue(Long attributeDefinitionId, String value);
    Optional<AttributeOption> findByIdAndAttributeDefinitionId(Long id, Long attributeDefinitionId);
}
