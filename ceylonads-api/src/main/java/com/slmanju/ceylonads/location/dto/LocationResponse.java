package com.slmanju.ceylonads.location.dto;

import com.slmanju.ceylonads.location.entity.LocationType;

public record LocationResponse(
        Long id,
        String name,
        String slug,
        LocationType type,
        Long parentId) {
}
