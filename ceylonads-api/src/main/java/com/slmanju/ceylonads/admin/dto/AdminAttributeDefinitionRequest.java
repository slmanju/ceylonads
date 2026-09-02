package com.slmanju.ceylonads.admin.dto;

import com.slmanju.ceylonads.category.entity.AttributeDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminAttributeDefinitionRequest(
        @NotBlank @Size(max = 60) @Pattern(regexp = "^[a-z][a-zA-Z0-9]*$",
                message = "Key must be camelCase, starting with a lowercase letter") String key,
        @NotBlank @Size(max = 100) String name,
        @NotNull AttributeDataType dataType,
        boolean required,
        boolean filterable,
        boolean searchable,
        @Size(max = 30) String unit,
        int displayOrder,
        List<AdminAttributeOptionRequest> options) {
}
