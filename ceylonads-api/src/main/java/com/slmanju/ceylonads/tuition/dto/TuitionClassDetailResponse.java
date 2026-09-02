package com.slmanju.ceylonads.tuition.dto;

import com.slmanju.ceylonads.ad.dto.AdContactResponse;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.media.dto.MediaResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// status is always ACTIVE on the public detail read (see TuitionClassService.getDetailBySlug,
// which only ever loads ACTIVE ads) but reflects the real lifecycle status on the owner-facing
// create/update/My Classes responses (see TuitionClassService.create/update/myClasses), where a
// listing is commonly PENDING_REVIEW awaiting moderation. source_channel is deliberately not
// included here - the /api/tuition/** endpoint already implies TUITION.
public record TuitionClassDetailResponse(
        Long id,
        String slug,
        String title,
        String description,
        BigDecimal price,
        String categorySlug,
        AdStatus status,
        Instant createdAt,
        Instant publishedAt,
        TuitionAcademicInfo academic,
        TuitionClassInfo classInfo,
        List<LocationResponse> locations,
        List<MediaResponse> media,
        AdContactResponse contact) {
}
