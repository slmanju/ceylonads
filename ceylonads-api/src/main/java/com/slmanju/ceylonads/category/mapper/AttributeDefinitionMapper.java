package com.slmanju.ceylonads.category.mapper;

import com.slmanju.ceylonads.category.dto.AttributeDefinitionResponse;
import com.slmanju.ceylonads.category.dto.AttributeOptionResponse;
import com.slmanju.ceylonads.category.entity.AttributeDefinition;
import com.slmanju.ceylonads.category.entity.AttributeOption;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AttributeDefinitionMapper {

    public AttributeOptionResponse toResponse(AttributeOption option) {
        return new AttributeOptionResponse(
                option.getId(), option.getValue(), option.getLabel(), option.getDisplayOrder(), option.isActive());
    }

    public AttributeDefinitionResponse toResponse(AttributeDefinition definition, List<AttributeOption> options) {
        return new AttributeDefinitionResponse(
                definition.getId(),
                definition.getCategory().getId(),
                definition.getKey(),
                definition.getName(),
                definition.getDataType(),
                definition.isRequired(),
                definition.isFilterable(),
                definition.getUnit(),
                definition.getDisplayOrder(),
                definition.isActive(),
                options.stream().map(this::toResponse).toList());
    }
}
