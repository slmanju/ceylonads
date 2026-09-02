package com.slmanju.ceylonads.category.dto;

public record AttributeOptionResponse(
        Long id,
        String value,
        String label,
        int displayOrder,
        boolean active) {
}
