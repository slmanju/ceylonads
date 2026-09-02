package com.slmanju.ceylonads.admin.dto;

import com.slmanju.ceylonads.location.entity.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminLocationRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 140) String slug,
        @NotNull LocationType type,
        String parentSlug) {
}
