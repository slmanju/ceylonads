package com.slmanju.ceylonads.ad.dto;

import com.slmanju.ceylonads.category.entity.AttributeDataType;

public record AdAttributeResponse(
        String key,
        String name,
        AttributeDataType dataType,
        String value,
        String displayValue,
        String unit) {
}
