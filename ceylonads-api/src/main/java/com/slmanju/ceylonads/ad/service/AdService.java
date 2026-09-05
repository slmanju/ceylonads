package com.slmanju.ceylonads.ad.service;

import com.slmanju.ceylonads.ad.dto.AdAttributeResponse;
import com.slmanju.ceylonads.ad.dto.AdResponse;
import com.slmanju.ceylonads.ad.dto.CreateAdRequest;
import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.mapper.AdMapper;
import com.slmanju.ceylonads.ad.repository.AdRepository;
import com.slmanju.ceylonads.auth.entity.Account;
import com.slmanju.ceylonads.auth.repository.AccountRepository;
import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.common.util.Slugs;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.customer.service.CustomerService;
import com.slmanju.ceylonads.location.dto.LocationResponse;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.repository.MediaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// MediaRepository is used directly here (rather than MediaService, which itself depends on
// AdService for ownership checks) to avoid a circular service dependency.
@Service
public class AdService {

    private final AdRepository ads;
    private final CategoryRepository categories;
    private final CustomerService customerService;
    private final AccountRepository accounts;
    private final AdMapper adMapper;
    private final AdAttributeService adAttributeService;
    private final AdLocationService adLocationService;
    private final MediaRepository mediaRepository;

    public AdService(
            AdRepository ads,
            CategoryRepository categories,
            CustomerService customerService,
            AccountRepository accounts,
            AdMapper adMapper,
            AdAttributeService adAttributeService,
            AdLocationService adLocationService,
            MediaRepository mediaRepository) {
        this.ads = ads;
        this.categories = categories;
        this.customerService = customerService;
        this.accounts = accounts;
        this.adMapper = adMapper;
        this.adAttributeService = adAttributeService;
        this.adLocationService = adLocationService;
        this.mediaRepository = mediaRepository;
    }

    // MAIN's own create endpoint - fixes the channel to MAIN_SITE so the main controller never has
    // to say so itself. This is the shared Ad-lifecycle core: a future Tuition/Boarding create
    // flow adds its own vertical-specific validation/mapping first, then calls the channel-aware
    // overload below directly - never a duplicated create implementation.
    @Transactional
    public AdResponse create(String username, CreateAdRequest request) {
        return create(username, request, SourceChannel.MAIN_SITE);
    }

    @Transactional
    public AdResponse create(String username, CreateAdRequest request, SourceChannel channel) {
        Ad ad = createAd(username, request, channel);
        // A brand-new ad can't have media yet (photos are uploaded in a separate step after
        // creation), so there's nothing to fetch here.
        return adMapper.toOwnerResponse(ad, false, List.of(), adAttributeService.toResponses(ad.getId()),
                adLocationService.toResponses(ad.getId()));
    }

    // The actual shared Ad-lifecycle core, returning the persisted entity rather than a DTO: every
    // vertical create flow (main today, Tuition/Boarding later) calls this directly after its own
    // validation/mapping, then shapes its own response from the result - persistence itself is
    // never duplicated. Seller resolution, category validation, attribute/location persistence,
    // and the transaction boundary all live here exactly once.
    @Transactional
    public Ad createAd(String username, CreateAdRequest request, SourceChannel channel) {
        Customer seller = customerService.requireByUsername(username);
        Category category = categories.findBySlugAndActiveTrue(request.categorySlug())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        Ad ad = new Ad(
                request.title().trim(),
                request.description().trim(),
                request.price(),
                category,
                seller);
        ad.assignSourceChannel(channel);
        ad = ads.save(ad);
        ad.updateContact(normalizeContact(request.contactName()), normalizeContact(request.phoneNumber()),
                normalizeContact(request.whatsappNumber()));
        adAttributeService.replaceValues(ad, request.attributes());
        adLocationService.replaceLocations(ad, resolveLocationSlugs(request), request.attributes());
        return ad;
    }

    // Blank means "clear the override, fall back to the account contact" - stored as null rather
    // than an empty string so AdMapper's fallback check (StringUtils.hasText) treats it the same
    // as never having been set.
    private String normalizeContact(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // Transitional single-location convenience: locationSlugs takes precedence when present.
    private List<String> resolveLocationSlugs(CreateAdRequest request) {
        if (request.locationSlugs() != null) {
            return request.locationSlugs();
        }
        if (request.locationSlug() != null && !request.locationSlug().isBlank()) {
            return List.of(request.locationSlug());
        }
        return List.of();
    }

    // Detail read path: one query for ad+category+seller, one for media, one for
    // attributes+definitions+options, one for locations.
    @Transactional(readOnly = true)
    public AdResponse getPublic(String idOrSlug) {
        Long id = Slugs.extractTrailingId(idOrSlug);
        if (id == null) {
            throw new NotFoundException("Ad not found");
        }
        // MAIN public marketplace boundary: a TUITION/BOARDING ad must not become reachable here
        // just by knowing its id (Tuition has its own isolated detail endpoint - see
        // TuitionClassService).
        Ad ad = ads.findDetailByIdAndStatusAndSourceChannel(id, AdStatus.ACTIVE, SourceChannel.MAIN_SITE)
                .orElseThrow(() -> new NotFoundException("Ad not found"));
        List<Media> media = mediaRepository.findByAdIdOrderByDisplayOrderAscIdAsc(ad.getId());
        List<AdAttributeResponse> attributes = adAttributeService.toResponses(ad.getId());
        List<LocationResponse> adLocations = adLocationService.toResponses(ad.getId());
        return adMapper.toDetailResponse(ad, false, media, attributes, adLocations);
    }

    @Transactional(readOnly = true)
    public List<AdResponse> mine(String username) {
        Customer customer = customerService.requireByUsername(username);
        List<Ad> myAds = ads.findBySellerIdOrderByCreatedAtDesc(customer.getId());
        return toResponses(myAds, false, adMapper::toOwnerResponse);
    }

    // Admin-only: lets an admin pick from a customer's own active ads when creating a promotion on
    // their behalf, without requiring the admin to know the customer's username/session.
    @Transactional(readOnly = true)
    public List<AdResponse> activeByCustomerId(Long customerId) {
        List<Ad> customerAds = ads.findBySellerIdAndStatusOrderByCreatedAtDesc(customerId, AdStatus.ACTIVE);
        return toResponses(customerAds, false);
    }

    // Admin-only variant of requireOwned: checks against a customer id rather than the acting
    // user's own username, since an admin is acting on a chosen customer's behalf.
    @Transactional(readOnly = true)
    public Ad requireOwnedByCustomer(Long id, Long customerId) {
        Ad ad = ads.findById(id).orElseThrow(() -> new NotFoundException("Ad not found"));
        if (!ad.getSeller().getId().equals(customerId)) {
            throw new BadRequestException("This ad does not belong to the selected customer");
        }
        return ad;
    }

    // MAIN's own update endpoint - fixes the expected channel to MAIN_SITE, so a customer can't
    // reach a TUITION/BOARDING ad they happen to own through this endpoint (404, matching
    // requireOwned's not-found shape - see below). Shared core, same reasoning as create() above:
    // a future Tuition/Boarding update flow calls the channel-aware overload directly.
    @Transactional
    public AdResponse updateOwned(Long id, String username, CreateAdRequest request) {
        return updateOwned(id, username, request, SourceChannel.MAIN_SITE);
    }

    @Transactional
    public AdResponse updateOwned(Long id, String username, CreateAdRequest request, SourceChannel expectedChannel) {
        Ad ad = updateAd(id, username, request, expectedChannel);
        List<Media> media = mediaRepository.findByAdIdOrderByDisplayOrderAscIdAsc(ad.getId());
        return adMapper.toOwnerResponse(ad, false, media, adAttributeService.toResponses(ad.getId()),
                adLocationService.toResponses(ad.getId()));
    }

    // Shared update core, same reasoning as createAd() above: returns the persisted entity so each
    // vertical shapes its own response, instead of duplicating ownership/category/attribute/
    // location persistence.
    @Transactional
    public Ad updateAd(Long id, String username, CreateAdRequest request, SourceChannel expectedChannel) {
        Ad ad = requireOwned(id, username, expectedChannel);
        Category category = categories.findBySlugAndActiveTrue(request.categorySlug())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        // ad.update() never touches sourceChannel - it stays exactly what requireOwned already
        // verified it was, matching the "preserve, never transfer" invariant.
        ad.update(request.title().trim(), request.description().trim(), request.price(), category);
        ad.updateContact(normalizeContact(request.contactName()), normalizeContact(request.phoneNumber()),
                normalizeContact(request.whatsappNumber()));
        adAttributeService.replaceValues(ad, request.attributes());
        adLocationService.replaceLocations(ad, resolveLocationSlugs(request), request.attributes());
        return ad;
    }

    @Transactional
    public void deactivateOwned(Long id, String username) {
        deactivateOwned(id, username, null);
    }

    // expectedChannel non-null: lets a vertical's own delete/deactivate endpoint (e.g. Tuition)
    // reuse the same ownership+channel check as its update endpoint, so it can't touch an ad
    // outside its own channel either.
    @Transactional
    public void deactivateOwned(Long id, String username, SourceChannel expectedChannel) {
        requireOwned(id, username, expectedChannel).deactivate();
    }

    // Channel-agnostic: used wherever ownership alone matters regardless of channel (promotions,
    // media uploads, deactivation). updateOwned() is the one caller that needs the channel-checked
    // variant below, since an update is scoped to a specific vertical's endpoint.
    @Transactional(readOnly = true)
    public Ad requireOwned(Long id, String username) {
        return requireOwned(id, username, null);
    }

    // expectedChannel non-null: 404s (never leaks that the ad exists in another channel) when the
    // owned ad doesn't belong to the channel this endpoint is scoped to.
    @Transactional(readOnly = true)
    public Ad requireOwned(Long id, String username, SourceChannel expectedChannel) {
        Ad ad = ads.findById(id).orElseThrow(() -> new NotFoundException("Ad not found"));
        if (!ad.getSeller().getAccount().getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("Not your ad");
        }
        if (expectedChannel != null && ad.getSourceChannel() != expectedChannel) {
            throw new NotFoundException("Ad not found");
        }
        return ad;
    }

    // Cross-channel: used by the ADMIN-only /api/admin/ads/** path (AdminService), which must see
    // every channel's pending queue.
    @Transactional(readOnly = true)
    public List<AdResponse> pendingReview() {
        return pendingReview(null);
    }

    // MAIN moderator queue when restrictToChannel is non-null (see ModerationController, which
    // passes null for an ADMIN caller and MAIN_SITE for a MODERATOR-only caller); shares the same
    // /api/moderation/ads/** endpoint as the ADMIN cross-channel case above.
    @Transactional(readOnly = true)
    public List<AdResponse> pendingReview(SourceChannel restrictToChannel) {
        return listByStatus(AdStatus.PENDING_REVIEW, restrictToChannel);
    }

    // Generalized form of pendingReview above, for the Tuition admin console's Classes tabs
    // (Pending/Active/Rejected/Expired) - any status, same channel-restriction shape.
    @Transactional(readOnly = true)
    public List<AdResponse> listByStatus(AdStatus status, SourceChannel restrictToChannel) {
        List<Ad> found = restrictToChannel == null
                ? ads.findByStatusOrderByCreatedAtAsc(status)
                : ads.findByStatusAndSourceChannelOrderByCreatedAtAsc(status, restrictToChannel);
        return toResponses(found, false);
    }

    // Open to any moderator/admin, including the ad's own creator - self-approval is an
    // intentional MVP allowance (see Ad.reviewedByAccountId), not an oversight.
    @Transactional
    public AdResponse approve(Long id, String reviewerUsername) {
        return approve(id, reviewerUsername, null);
    }

    // restrictToChannel non-null: rejects (404, matching requireAny's not-found shape - never
    // leaks that the ad exists in another channel) an attempt to approve an ad outside that
    // channel. Used by ModerationController for a MODERATOR-only caller.
    @Transactional
    public AdResponse approve(Long id, String reviewerUsername, SourceChannel restrictToChannel) {
        Ad ad = requireAny(id, restrictToChannel);
        ad.approve(requireAccountId(reviewerUsername));
        return toSingleResponse(ad, false);
    }

    @Transactional
    public AdResponse reject(Long id, String reviewerUsername) {
        return reject(id, reviewerUsername, null);
    }

    @Transactional
    public AdResponse reject(Long id, String reviewerUsername, SourceChannel restrictToChannel) {
        Ad ad = requireAny(id, restrictToChannel);
        ad.reject(requireAccountId(reviewerUsername));
        return toSingleResponse(ad, false);
    }

    // Admin detail read for a specific channel (e.g. the Tuition admin's pending-class review
    // view) - same requireAny channel guard as approve/reject/adminDeactivate below, so a caller
    // restricted to one channel 404s (not leaks) on an id from another channel.
    @Transactional(readOnly = true)
    public AdResponse getForAdmin(Long id, SourceChannel restrictToChannel) {
        return toSingleResponse(requireAny(id, restrictToChannel), false);
    }

    @Transactional
    public AdResponse adminDeactivate(Long id) {
        return adminDeactivate(id, null);
    }

    @Transactional
    public AdResponse adminDeactivate(Long id, SourceChannel restrictToChannel) {
        Ad ad = requireAny(id, restrictToChannel);
        ad.deactivate();
        return toSingleResponse(ad, false);
    }

    // TuitionExpiryScheduler's hourly sweep: flips stale ACTIVE ads in the given channel past
    // their expiresAt to EXPIRED, via a bulk update - see AdRepository.expireOverdue.
    @Transactional
    public int expireOverdue(SourceChannel channel) {
        return ads.expireOverdue(channel, Instant.now());
    }

    // Public (not just used internally by approve/reject/getForAdmin/adminDeactivate above): also
    // used by PromotionService#createAdminPromotionForTuitionClass to resolve the class entity
    // (owner, status, expiry) for the Tuition admin console's "Promote Class" action - same 404,
    // never-leak-cross-channel-existence shape as every other admin lookup here.
    @Transactional(readOnly = true)
    public Ad requireAny(Long id, SourceChannel restrictToChannel) {
        Ad ad = ads.findDetailById(id).orElseThrow(() -> new NotFoundException("Ad not found"));
        if (restrictToChannel != null && ad.getSourceChannel() != restrictToChannel) {
            throw new NotFoundException("Ad not found");
        }
        return ad;
    }

    private Long requireAccountId(String username) {
        Account account = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return account.getId();
    }

    private AdResponse toSingleResponse(Ad ad, boolean promoted) {
        List<Media> media = mediaRepository.findByAdIdOrderByDisplayOrderAscIdAsc(ad.getId());
        return adMapper.toResponse(ad, promoted, media, adAttributeService.toResponses(ad.getId()),
                adLocationService.toResponses(ad.getId()));
    }

    // Batches media, attributes, and locations for a whole list of ads into three queries total
    // (regardless of list size) instead of three queries per ad.
    private List<AdResponse> toResponses(List<Ad> adList, boolean promoted) {
        return toResponses(adList, promoted, adMapper::toResponse);
    }

    // Same batching, but lets callers (e.g. mine(), which needs owner-only contact override data)
    // swap in a different AdMapper method without duplicating the query-batching logic.
    private List<AdResponse> toResponses(List<Ad> adList, boolean promoted, AdResponseMapper mapper) {
        if (adList.isEmpty()) {
            return List.of();
        }
        List<Long> ids = adList.stream().map(Ad::getId).toList();
        Map<Long, List<Media>> mediaByAdId = new LinkedHashMap<>();
        for (Media media : mediaRepository.findByAdIdInOrderByAdIdAscDisplayOrderAscIdAsc(ids)) {
            mediaByAdId.computeIfAbsent(media.getAd().getId(), k -> new ArrayList<>()).add(media);
        }
        Map<Long, List<AdAttributeResponse>> attributesByAdId = adAttributeService.toResponsesForAds(ids);
        Map<Long, List<LocationResponse>> locationsByAdId = adLocationService.toResponsesForAds(ids);
        return adList.stream()
                .map(ad -> mapper.map(
                        ad,
                        promoted,
                        mediaByAdId.getOrDefault(ad.getId(), List.of()),
                        attributesByAdId.getOrDefault(ad.getId(), List.of()),
                        locationsByAdId.getOrDefault(ad.getId(), List.of())))
                .toList();
    }

    @FunctionalInterface
    private interface AdResponseMapper {
        AdResponse map(Ad ad, boolean promoted, List<Media> media, List<AdAttributeResponse> attributes, List<LocationResponse> locations);
    }
}
