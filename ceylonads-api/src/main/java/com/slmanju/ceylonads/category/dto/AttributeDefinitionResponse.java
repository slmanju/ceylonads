package com.slmanju.ceylonads.category.dto;

import com.slmanju.ceylonads.category.entity.AttributeDataType;

import java.util.List;

public record AttributeDefinitionResponse(
        Long id,
        Long categoryId,
        String key,
        String name,
        AttributeDataType dataType,
        boolean required,
        boolean filterable,
        String unit,
        int displayOrder,
        boolean active,
        List<AttributeOptionResponse> options) {
}
