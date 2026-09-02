package com.slmanju.ceylonads.ad.dto;

import com.slmanju.ceylonads.common.util.Phones;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreateAdRequest(
        @NotBlank @Size(max = 180) String title,
        // Description is optional: an empty ad description is a valid listing (e.g. the photos
        // and title/attributes already say enough), so this only bounds the maximum length.
        @NotNull @Size(max = 5000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotBlank String categorySlug,
        // Transitional single-location convenience, superseded by locationSlugs below; still
        // accepted so older clients/tests keep working. Ignored once locationSlugs is present.
        String locationSlug,
        // 0..N location slugs; whether zero/one/many is actually allowed is a category-dependent
        // business rule enforced by AdLocationService, not by validation here.
        List<String> locationSlugs,
        Map<String, String> attributes,
        // Ad-specific contact override, all optional: a blank/omitted value falls back to the
        // seller's account contact (see AdMapper.resolveContact). Lets a seller list a contractor,
        // agent, or family member's number instead of their own account phone.
        @Size(max = 120) String contactName,
        @Pattern(regexp = Phones.SRI_LANKAN_PHONE_PATTERN, message = "Enter a valid Sri Lankan phone number, e.g. 0712345678 or +94712345678")
        String phoneNumber,
        @Pattern(regexp = Phones.SRI_LANKAN_PHONE_PATTERN, message = "Enter a valid Sri Lankan phone number, e.g. 0712345678 or +94712345678")
        String whatsappNumber) {
}
