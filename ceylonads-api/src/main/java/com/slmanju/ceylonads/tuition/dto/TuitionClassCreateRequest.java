package com.slmanju.ceylonads.tuition.dto;

import com.slmanju.ceylonads.common.util.Phones;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

// Used for both POST /api/tuition/classes and PUT /api/tuition/classes/{id} - same convention as
// the generic CreateAdRequest, which the main /api/ads/** create/update endpoints both accept.
//
// Deliberately has no sourceChannel field: the endpoint itself (TuitionClassService.create/update)
// is what assigns/requires TUITION, never the request payload - see AdService.createAd/updateAd.
//
// Tuition attribute fields (subject/level/curriculum/medium/deliveryMode/classFormat) are all
// optional here and mapped onto the existing subject/grade/curriculum/medium/classMode/classType
// attribute_definitions keys: which of them are actually required, and what values are valid, is a
// category-dependent master-data concern (the ~10 leaf categories under education-tuition don't
// all define the same attributes) enforced by the existing shared AdAttributeService, not
// duplicated here.
public record TuitionClassCreateRequest(
        @NotBlank @Size(max = 180) String title,
        @NotNull @Size(max = 5000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotBlank String categorySlug,
        // 0..N location slugs; cardinality (e.g. online classes need none) is enforced by the
        // existing AdLocationService, which already has Tuition-specific rules keyed off classMode.
        List<String> locationSlugs,
        @Size(max = 120) String contactName,
        @Pattern(regexp = Phones.SRI_LANKAN_PHONE_PATTERN, message = "Enter a valid Sri Lankan phone number, e.g. 0712345678 or +94712345678")
        String phoneNumber,
        @Pattern(regexp = Phones.SRI_LANKAN_PHONE_PATTERN, message = "Enter a valid Sri Lankan phone number, e.g. 0712345678 or +94712345678")
        String whatsappNumber,
        String subject,
        String level,
        String curriculum,
        List<String> medium,
        String deliveryMode,
        String classFormat) {
}
