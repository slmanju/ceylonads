package com.slmanju.ceylonads.location.service;

import com.slmanju.ceylonads.location.entity.Location;
import com.slmanju.ceylonads.location.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the location tree (province/district/city) in memory from a single query, mirroring
 * CategoryHierarchyService, so a search request needs a fixed number of round trips regardless of
 * how many descendants a location has.
 */
@Service
public class LocationHierarchyService {

    private final LocationRepository locations;

    public LocationHierarchyService(LocationRepository locations) {
        this.locations = locations;
    }

    // Selecting a parent location (e.g. a province) must match it and every descendant location
    // (district, city) at any depth; selecting a leaf just matches that one location.
    @Transactional(readOnly = true)
    public Set<Long> descendantIdsInclusive(Location root) {
        Map<Long, List<Location>> childrenByParentId = locations.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(l -> l.getParent() != null)
                .collect(Collectors.groupingBy(l -> l.getParent().getId()));

        Set<Long> ids = new LinkedHashSet<>();
        Deque<Location> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Location current = stack.pop();
            if (ids.add(current.getId())) {
                for (Location child : childrenByParentId.getOrDefault(current.getId(), List.of())) {
                    stack.push(child);
                }
            }
        }
        return ids;
    }
}
