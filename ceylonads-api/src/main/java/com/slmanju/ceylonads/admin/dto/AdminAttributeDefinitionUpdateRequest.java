package com.slmanju.ceylonads.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAttributeDefinitionUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        boolean required,
        boolean filterable,
        boolean searchable,
        @Size(max = 30) String unit,
        int displayOrder,
        boolean active) {
}
