package com.slmanju.ceylonads.category.dto;

import java.util.List;

public record CategoryFiltersResponse(
        CategoryResponse category,
        List<AttributeDefinitionResponse> filters) {
}
