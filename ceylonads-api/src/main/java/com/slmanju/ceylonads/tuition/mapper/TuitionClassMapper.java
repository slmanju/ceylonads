package com.slmanju.ceylonads.tuition.mapper;

import com.slmanju.ceylonads.ad.dto.AdContactResponse;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdAttributeValue;
import com.slmanju.ceylonads.category.entity.AttributeOption;
import com.slmanju.ceylonads.common.util.Slugs;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.media.dto.MediaResponse;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.mapper.MediaMapper;
import com.slmanju.ceylonads.tuition.dto.AttributeValueLabel;
import com.slmanju.ceylonads.tuition.dto.TuitionAcademicInfo;
import com.slmanju.ceylonads.tuition.dto.TuitionClassCardResponse;
import com.slmanju.ceylonads.tuition.dto.TuitionClassDetailResponse;
import com.slmanju.ceylonads.tuition.dto.TuitionClassInfo;
import com.slmanju.ceylonads.tuition.dto.TuitionFeaturedCardResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Pure transformation only, same shape as AdMapper: takes the ad plus its already-loaded
// media/attributes/locations and shapes the tuition-specific response, so a mapper call never
// triggers its own database round trip.
@Component
public class TuitionClassMapper {

    private final MediaMapper mediaMapper;

    public TuitionClassMapper(MediaMapper mediaMapper) {
        this.mediaMapper = mediaMapper;
    }

    public TuitionClassDetailResponse toDetailResponse(
            Ad ad,
            List<Media> media,
            List<AdAttributeValue> attributeRows,
            Map<Long, List<AttributeOption>> optionsByDefinition,
            List<LocationResponse> locations) {
        Map<String, List<AdAttributeValue>> byKey = groupByKey(attributeRows);

        TuitionAcademicInfo academic = new TuitionAcademicInfo(
                labelValue(byKey, "subject", optionsByDefinition),
                labelValue(byKey, "grade", optionsByDefinition),
                singleLabel(byKey, "curriculum", optionsByDefinition),
                multiLabel(byKey, "medium", optionsByDefinition));

        TuitionClassInfo classInfo = new TuitionClassInfo(
                wrapSingle(singleLabel(byKey, "classMode", optionsByDefinition)),
                wrapSingle(singleLabel(byKey, "classType", optionsByDefinition)),
                List.of());

        List<MediaResponse> mediaResponses = media.stream().map(mediaMapper::toResponse).toList();

        return new TuitionClassDetailResponse(
                ad.getId(),
                Slugs.adSlug(ad.getTitle(), ad.getId()),
                ad.getTitle(),
                ad.getDescription(),
                ad.getPrice(),
                ad.getCategory().getSlug(),
                ad.getStatus(),
                ad.getCreatedAt(),
                ad.getPublishedAt(),
                ad.getExpiresAt(),
                academic,
                classInfo,
                locations,
                mediaResponses,
                resolveContact(ad));
    }

    public TuitionClassCardResponse toCardResponse(
            Ad ad,
            Media primaryMedia,
            List<AdAttributeValue> attributeRows,
            Map<Long, List<AttributeOption>> optionsByDefinition,
            LocationResponse primaryLocation) {
        Map<String, List<AdAttributeValue>> byKey = groupByKey(attributeRows);

        return new TuitionClassCardResponse(
                ad.getId(),
                Slugs.adSlug(ad.getTitle(), ad.getId()),
                ad.getTitle(),
                ad.getPrice(),
                primaryMedia == null ? null : mediaMapper.toResponse(primaryMedia).url(),
                primaryLocation,
                labelValue(byKey, "subject", optionsByDefinition),
                labelValue(byKey, "grade", optionsByDefinition),
                singleLabel(byKey, "curriculum", optionsByDefinition),
                multiLabel(byKey, "medium", optionsByDefinition));
    }

    // Featured Tuition carousel card - GET /api/tuition/featured. Reuses the same label-resolution
    // helpers as toCardResponse, plus classMode (as deliveryMode) and the seller's display name,
    // which the featured carousel design needs but the plain class card does not.
    public TuitionFeaturedCardResponse toFeaturedCardResponse(
            Ad ad,
            Media primaryMedia,
            List<AdAttributeValue> attributeRows,
            Map<Long, List<AttributeOption>> optionsByDefinition,
            LocationResponse primaryLocation) {
        Map<String, List<AdAttributeValue>> byKey = groupByKey(attributeRows);

        return new TuitionFeaturedCardResponse(
                ad.getId(),
                Slugs.adSlug(ad.getTitle(), ad.getId()),
                ad.getTitle(),
                ad.getPrice(),
                primaryMedia == null ? null : mediaMapper.toResponse(primaryMedia).url(),
                primaryLocation,
                labelValue(byKey, "subject", optionsByDefinition),
                labelValue(byKey, "grade", optionsByDefinition),
                singleLabel(byKey, "curriculum", optionsByDefinition),
                multiLabel(byKey, "medium", optionsByDefinition),
                singleLabel(byKey, "classMode", optionsByDefinition),
                ad.getSeller().getDisplayName());
    }

    private Map<String, List<AdAttributeValue>> groupByKey(List<AdAttributeValue> rows) {
        Map<String, List<AdAttributeValue>> byKey = new LinkedHashMap<>();
        for (AdAttributeValue row : rows) {
            byKey.computeIfAbsent(row.getAttributeDefinition().getKey(), k -> new ArrayList<>()).add(row);
        }
        return byKey;
    }

    // subject/grade resolve through the option label map like the other SELECT attributes below
    // rather than exposing the raw stored value (e.g. "MATHEMATICS") to the UI.
    private String labelValue(
            Map<String, List<AdAttributeValue>> byKey, String key, Map<Long, List<AttributeOption>> optionsByDefinition) {
        AttributeValueLabel label = singleLabel(byKey, key, optionsByDefinition);
        return label == null ? null : label.label();
    }

    private AttributeValueLabel singleLabel(
            Map<String, List<AdAttributeValue>> byKey, String key, Map<Long, List<AttributeOption>> optionsByDefinition) {
        List<AdAttributeValue> rows = byKey.get(key);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return toLabel(rows.get(0), optionsByDefinition);
    }

    private List<AttributeValueLabel> multiLabel(
            Map<String, List<AdAttributeValue>> byKey, String key, Map<Long, List<AttributeOption>> optionsByDefinition) {
        List<AdAttributeValue> rows = byKey.get(key);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(row -> toLabel(row, optionsByDefinition)).toList();
    }

    private List<AttributeValueLabel> wrapSingle(AttributeValueLabel label) {
        return label == null ? List.of() : List.of(label);
    }

    private AttributeValueLabel toLabel(AdAttributeValue row, Map<Long, List<AttributeOption>> optionsByDefinition) {
        String value = row.getValueText();
        List<AttributeOption> defOptions = optionsByDefinition.getOrDefault(row.getAttributeDefinition().getId(), List.of());
        String label = defOptions.stream()
                .filter(o -> o.getValue().equals(value))
                .map(AttributeOption::getLabel)
                .findFirst()
                .orElse(value);
        return new AttributeValueLabel(value, label);
    }

    // Mirrors AdMapper.resolveContact's per-field fallback (ad override, else seller account
    // contact); kept as a small local copy since AdMapper keeps that method private to its package.
    private AdContactResponse resolveContact(Ad ad) {
        Customer seller = ad.getSeller();
        String name = StringUtils.hasText(ad.getContactName()) ? ad.getContactName() : seller.getDisplayName();
        String phone = StringUtils.hasText(ad.getPhoneNumber()) ? ad.getPhoneNumber() : seller.getPhone();
        String whatsapp = StringUtils.hasText(ad.getWhatsappNumber()) ? ad.getWhatsappNumber() : seller.getPhone();
        return new AdContactResponse(name, phone, whatsapp);
    }
}
