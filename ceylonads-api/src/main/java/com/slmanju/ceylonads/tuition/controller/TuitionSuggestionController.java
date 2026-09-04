package com.slmanju.ceylonads.tuition.controller;

import com.slmanju.ceylonads.tuition.dto.TuitionSuggestionCreateRequest;
import com.slmanju.ceylonads.tuition.service.TuitionSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Public - permitted without authentication (see SecurityConfig, which permits POST here
// specifically since /api/tuition/** is otherwise only permitAll for GET).
@RestController
@RequestMapping("/api/tuition/suggestions")
public class TuitionSuggestionController {

    private final TuitionSuggestionService suggestionService;

    public TuitionSuggestionController(TuitionSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a public suggestion/feedback message",
            description = "No response body - the public page never needs to know the internal suggestion id.")
    void create(@Valid @RequestBody TuitionSuggestionCreateRequest request) {
        suggestionService.create(request);
    }
}
