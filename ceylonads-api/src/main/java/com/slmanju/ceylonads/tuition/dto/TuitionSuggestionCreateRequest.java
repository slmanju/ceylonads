package com.slmanju.ceylonads.tuition.dto;

import com.slmanju.ceylonads.common.util.Phones;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Public "Suggest" page submission. Deliberately has no status/reviewedAt/reviewedBy fields -
// those are only ever set server-side (see TuitionSuggestion.markReviewed/markClosed), so a
// client can't set them even by including extra JSON properties.
public record TuitionSuggestionCreateRequest(
        @Size(max = 120) String name,
        @Size(max = 180) @Email(message = "Enter a valid email address") String email,
        @Pattern(regexp = Phones.SRI_LANKAN_PHONE_PATTERN, message = "Enter a valid Sri Lankan phone number, e.g. 0712345678 or +94712345678")
        String phone,
        @NotBlank(message = "Please enter your suggestion or feedback")
        @Size(max = 2000, message = "Suggestion must be 2000 characters or fewer")
        String message) {
}
