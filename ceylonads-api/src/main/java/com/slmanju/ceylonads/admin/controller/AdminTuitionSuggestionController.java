package com.slmanju.ceylonads.admin.controller;

import com.slmanju.ceylonads.tuition.dto.TuitionSuggestionAdminResponse;
import com.slmanju.ceylonads.tuition.entity.SuggestionStatus;
import com.slmanju.ceylonads.tuition.service.TuitionSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// /api/admin/** is ROLE_ADMIN-only centrally in SecurityConfig - no separate authorization check
// needed here (mirrors AdminController).
@RestController
@RequestMapping("/api/admin/tuition/suggestions")
@SecurityRequirement(name = "bearerAuth")
public class AdminTuitionSuggestionController {

    private final TuitionSuggestionService suggestionService;

    public AdminTuitionSuggestionController(TuitionSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping
    @Operation(summary = "List suggestions", description = "NEW first, then REVIEWED, then CLOSED; newest first within each group.")
    List<TuitionSuggestionAdminResponse> list() {
        return suggestionService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single suggestion")
    TuitionSuggestionAdminResponse get(@PathVariable Long id) {
        return suggestionService.get(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Mark a suggestion Reviewed or Closed")
    TuitionSuggestionAdminResponse updateStatus(
            Authentication authentication, @PathVariable Long id, @RequestParam SuggestionStatus status) {
        return suggestionService.updateStatus(id, status, authentication.getName());
    }
}
