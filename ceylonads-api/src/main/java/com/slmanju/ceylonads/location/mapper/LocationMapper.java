package com.slmanju.ceylonads.location.mapper;

import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.location.entity.Location;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getSlug(),
                location.getType(),
                location.getParent() == null ? null : location.getParent().getId());
    }
}
