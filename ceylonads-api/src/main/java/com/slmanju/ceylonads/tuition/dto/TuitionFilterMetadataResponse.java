package com.slmanju.ceylonads.tuition.dto;

import java.util.List;

// Master options for the Tuition search UI's filter panel.
public record TuitionFilterMetadataResponse(
        List<TuitionFilterOptionResponse> subjects,
        List<TuitionFilterOptionResponse> levels,
        List<TuitionFilterOptionResponse> curricula,
        List<TuitionFilterOptionResponse> mediums,
        List<TuitionFilterOptionResponse> deliveryModes) {
}
