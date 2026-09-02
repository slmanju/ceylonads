package com.slmanju.ceylonads.location.service;

import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.location.entity.Location;
import com.slmanju.ceylonads.location.entity.LocationType;
import com.slmanju.ceylonads.location.mapper.LocationMapper;
import com.slmanju.ceylonads.location.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocationService {

    private final LocationRepository locations;
    private final LocationMapper locationMapper;

    public LocationService(LocationRepository locations, LocationMapper locationMapper) {
        this.locations = locations;
        this.locationMapper = locationMapper;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> findAllActive() {
        return locations.findAllByActiveTrueOrderByNameAsc()
                .stream().map(locationMapper::toResponse).toList();
    }

    @Transactional
    public LocationResponse create(String name, String slug, LocationType type, String parentSlug) {
        if (locations.findBySlug(slug).isPresent()) {
            throw new BadRequestException("Location slug already exists");
        }
        Location parent = parentSlug == null || parentSlug.isBlank()
                ? null
                : locations.findBySlug(parentSlug)
                    .orElseThrow(() -> new NotFoundException("Parent location not found"));

        Location location = locations.save(new Location(name.trim(), slug.trim().toLowerCase(), type, parent));
        return locationMapper.toResponse(location);
    }
}
