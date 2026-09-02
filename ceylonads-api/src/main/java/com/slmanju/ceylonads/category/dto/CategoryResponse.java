package com.slmanju.ceylonads.category.dto;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        Long parentId,
        int displayOrder,
        boolean active) {
}
