package com.slmanju.ceylonads.tuition.service;

import com.slmanju.ceylonads.auth.entity.Account;
import com.slmanju.ceylonads.auth.repository.AccountRepository;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.tuition.dto.TuitionSuggestionAdminResponse;
import com.slmanju.ceylonads.tuition.dto.TuitionSuggestionCreateRequest;
import com.slmanju.ceylonads.tuition.entity.SuggestionStatus;
import com.slmanju.ceylonads.tuition.entity.TuitionSuggestion;
import com.slmanju.ceylonads.tuition.repository.TuitionSuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TuitionSuggestionService {

    private final TuitionSuggestionRepository suggestions;
    private final AccountRepository accounts;

    public TuitionSuggestionService(TuitionSuggestionRepository suggestions, AccountRepository accounts) {
        this.suggestions = suggestions;
        this.accounts = accounts;
    }

    @Transactional
    public void create(TuitionSuggestionCreateRequest request) {
        TuitionSuggestion suggestion = new TuitionSuggestion(
                normalize(request.name()), normalize(request.email()), normalize(request.phone()),
                request.message().trim());
        suggestions.save(suggestion);
    }

    @Transactional(readOnly = true)
    public List<TuitionSuggestionAdminResponse> list() {
        return suggestions.findAllOrderByStatusPriorityThenNewest().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TuitionSuggestionAdminResponse get(Long id) {
        return toResponse(require(id));
    }

    // Only REVIEWED/CLOSED are valid admin-initiated transitions; NEW is the create-only initial
    // state and is never a target status here.
    @Transactional
    public TuitionSuggestionAdminResponse updateStatus(Long id, SuggestionStatus newStatus, String reviewerUsername) {
        TuitionSuggestion suggestion = require(id);
        Long reviewerAccountId = requireAccountId(reviewerUsername);
        switch (newStatus) {
            case REVIEWED -> suggestion.markReviewed(reviewerAccountId);
            case CLOSED -> suggestion.markClosed(reviewerAccountId);
            case NEW -> throw new BadRequestException("A suggestion cannot be moved back to NEW");
        }
        return toResponse(suggestion);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private TuitionSuggestion require(Long id) {
        return suggestions.findById(id).orElseThrow(() -> new NotFoundException("Suggestion not found"));
    }

    private Long requireAccountId(String username) {
        Account account = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return account.getId();
    }

    private TuitionSuggestionAdminResponse toResponse(TuitionSuggestion suggestion) {
        return new TuitionSuggestionAdminResponse(
                suggestion.getId(), suggestion.getName(), suggestion.getEmail(), suggestion.getPhone(),
                suggestion.getMessage(), suggestion.getStatus(), suggestion.getCreatedAt(),
                suggestion.getReviewedAt(), suggestion.getReviewedByAccountId());
    }
}
