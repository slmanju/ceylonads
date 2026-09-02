package com.slmanju.ceylonads.location.controller;

import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.location.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @Operation(summary = "List active Sri Lankan locations")
    List<LocationResponse> list() {
        return locationService.findAllActive();
    }
}
