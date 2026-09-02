package com.slmanju.ceylonads.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 120) String slug,
        String parentSlug,
        int displayOrder) {
}
