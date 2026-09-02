package com.slmanju.ceylonads.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAttributeOptionRequest(
        @NotBlank @Size(max = 60) String value,
        @NotBlank @Size(max = 100) String label,
        int displayOrder) {
}
