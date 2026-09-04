package com.slmanju.ceylonads.ad.mapper;

import com.slmanju.ceylonads.ad.dto.AdAttributeResponse;
import com.slmanju.ceylonads.ad.dto.AdContactOverrideResponse;
import com.slmanju.ceylonads.ad.dto.AdContactResponse;
import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.ad.dto.AdSellerResponse;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.common.util.Slugs;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.media.dto.MediaResponse;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.mapper.MediaMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

// Pure transformation only: takes the ad plus its already-loaded media/attributes and shapes an
// AdResponse. Deliberately has no repository/service dependency of its own - callers (AdService,
// AdSearchService, PromotionService) are responsible for batching that data first, so a mapper
// invocation never triggers its own database round trip.
@Component
public class AdMapper {

    private final MediaMapper mediaMapper;

    public AdMapper(MediaMapper mediaMapper) {
        this.mediaMapper = mediaMapper;
    }

    // The promoted flag is contextual (it depends on which placement/ranking query produced
    // this ad), so callers that know an ad is currently boosted pass it in explicitly rather
    // than this mapper re-deriving it from the promotion domain on every call.
    //
    // No per-ad contact data here: search/list responses don't need it (see AdContactResponse),
    // so callers that do (public detail, ad owner) use toDetailResponse/toOwnerResponse below.
    public AdResponse toResponse(
            Ad ad, boolean promoted, List<Media> media, List<AdAttributeResponse> attributes, List<LocationResponse> locations) {
        return build(ad, promoted, media, attributes, locations, null, null);
    }

    // Public ad detail: adds the resolved effective contact (ad override, falling back to the
    // seller's account contact) that buyers use to Call/WhatsApp.
    public AdResponse toDetailResponse(
            Ad ad, boolean promoted, List<Media> media, List<AdAttributeResponse> attributes, List<LocationResponse> locations) {
        return build(ad, promoted, media, attributes, locations, resolveContact(ad), null);
    }

    // Returned to the ad's own owner (create/update/mine): adds the raw contact override as
    // stored, so Edit can tell "no override" apart from "override matches the account value".
    public AdResponse toOwnerResponse(
            Ad ad, boolean promoted, List<Media> media, List<AdAttributeResponse> attributes, List<LocationResponse> locations) {
        return build(ad, promoted, media, attributes, locations, null, toContactOverride(ad));
    }

    private AdResponse build(
            Ad ad, boolean promoted, List<Media> media, List<AdAttributeResponse> attributes, List<LocationResponse> locations,
            AdContactResponse contact, AdContactOverrideResponse contactOverride) {
        List<MediaResponse> mediaResponses = media.stream().map(mediaMapper::toResponse).toList();

        return new AdResponse(
                ad.getId(),
                Slugs.adSlug(ad.getTitle(), ad.getId()),
                ad.getTitle(),
                ad.getDescription(),
                ad.getPrice(),
                ad.getCategory().getName(),
                ad.getCategory().getSlug(),
                locations,
                toSellerResponse(ad.getSeller()),
                ad.getStatus(),
                ad.getCreatedAt(),
                ad.getPublishedAt(),
                ad.getExpiresAt(),
                ad.getReviewedAt(),
                mediaResponses,
                promoted,
                attributes,
                contact,
                contactOverride);
    }

    // Phase 1 assumes any phone a seller has on file is intended to be shown on their ads.
    // TODO: honor a per-seller contact-visibility preference once that concept exists.
    private AdSellerResponse toSellerResponse(Customer seller) {
        return new AdSellerResponse(seller.getId(), seller.getDisplayName(), seller.getPhone());
    }

    // Per-field fallback: an ad override wins when present, otherwise the seller's account
    // contact. The account has no separate WhatsApp field, so its phone doubles as the WhatsApp
    // fallback too.
    private AdContactResponse resolveContact(Ad ad) {
        Customer seller = ad.getSeller();
        String name = StringUtils.hasText(ad.getContactName()) ? ad.getContactName() : seller.getDisplayName();
        String phone = StringUtils.hasText(ad.getPhoneNumber()) ? ad.getPhoneNumber() : seller.getPhone();
        String whatsapp = StringUtils.hasText(ad.getWhatsappNumber()) ? ad.getWhatsappNumber() : seller.getPhone();
        return new AdContactResponse(name, phone, whatsapp);
    }

    private AdContactOverrideResponse toContactOverride(Ad ad) {
        return new AdContactOverrideResponse(ad.getContactName(), ad.getPhoneNumber(), ad.getWhatsappNumber());
    }
}
