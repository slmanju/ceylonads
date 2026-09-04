package com.slmanju.ceylonads.tuition.entity;

import jakarta.persistence.*;

import java.time.Instant;

// Public ezClass "Suggest" page feedback inbox - unrelated to Ad/Payment, so it gets its own
// table rather than being squeezed into an existing entity (see V28__tuition_suggestions.sql).
@Entity
@Table(name = "tuition_suggestions", indexes = {
        @Index(name = "idx_tuition_suggestions_status_created", columnList = "status,created_at")
})
public class TuitionSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 120)
    private String name;

    @Column(length = 180)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SuggestionStatus status = SuggestionStatus.NEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by_account_id")
    private Long reviewedByAccountId;

    protected TuitionSuggestion() {
    }

    public TuitionSuggestion(String name, String email, String phone, String message) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.message = message;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getMessage() { return message; }
    public SuggestionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Long getReviewedByAccountId() { return reviewedByAccountId; }

    // Server-driven transition only - reviewerAccountId/reviewedAt are never client-supplied (see
    // TuitionSuggestionCreateRequest, which has no status/review fields at all).
    public void markReviewed(Long reviewerAccountId) {
        this.status = SuggestionStatus.REVIEWED;
        this.reviewedAt = Instant.now();
        this.reviewedByAccountId = reviewerAccountId;
    }

    public void markClosed(Long reviewerAccountId) {
        this.status = SuggestionStatus.CLOSED;
        this.reviewedAt = Instant.now();
        this.reviewedByAccountId = reviewerAccountId;
    }
}
