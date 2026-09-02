package com.slmanju.ceylonads.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAttributeOptionUpdateRequest(
        @NotBlank @Size(max = 100) String label,
        int displayOrder,
        boolean active) {
}
