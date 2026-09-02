package com.slmanju.ceylonads.media.service;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.service.AdService;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.media.dto.MediaResponse;
import com.slmanju.ceylonads.media.dto.StoredMedia;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.entity.MediaOwnerType;
import com.slmanju.ceylonads.media.mapper.MediaMapper;
import com.slmanju.ceylonads.media.repository.MediaRepository;
import com.slmanju.ceylonads.media.storage.MediaStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MediaService {

    // Receipts are verified manually by an admin, so the format is restricted to formats
    // that render reliably in the review screen rather than the broader "any image/*" rule
    // ad photos use.
    private static final Set<String> RECEIPT_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final MediaStorage storage;
    private final MediaRepository mediaRepository;
    private final AdService adService;
    private final MediaMapper mediaMapper;

    public MediaService(MediaStorage storage, MediaRepository mediaRepository, AdService adService, MediaMapper mediaMapper) {
        this.storage = storage;
        this.mediaRepository = mediaRepository;
        this.adService = adService;
        this.mediaMapper = mediaMapper;
    }

    @Transactional
    public MediaResponse upload(Long adId, String username, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are supported");
        }
        long count = mediaRepository.countByAdId(adId);
        if (count >= 8) {
            throw new BadRequestException("Maximum 8 images per ad");
        }

        Ad ad = adService.requireOwned(adId, username);
        StoredMedia stored = storage.store(file.getInputStream(), file.getOriginalFilename(), contentType);
        Media media = mediaRepository.save(new Media(ad, stored.storageKey(), stored.contentType(), (int) count));
        return mediaMapper.toResponse(media);
    }

    /**
     * Stores a bank-transfer receipt image and returns the persisted {@link Media} entity so the
     * payment domain can attach it to a {@code Payment} within its own transaction. Ownership of
     * the payment is validated by {@code PaymentService} before this is called; this method only
     * deals with the file itself.
     */
    @Transactional
    public Media storePaymentReceipt(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !RECEIPT_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Receipt must be a JPEG, PNG, or WEBP image");
        }
        StoredMedia stored = storage.store(file.getInputStream(), file.getOriginalFilename(), contentType);
        return mediaRepository.save(new Media(stored.storageKey(), stored.contentType()));
    }

    /**
     * Uploads a banner image for an admin-created BANNER_PROMOTION. Reuses the same storage
     * abstraction as ad photos and payment receipts rather than a dedicated banner CMS.
     */
    @Transactional
    public MediaResponse uploadPromotionBanner(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are supported");
        }
        StoredMedia stored = storage.store(file.getInputStream(), file.getOriginalFilename(), contentType);
        Media media = mediaRepository.save(Media.forPromotionBanner(stored.storageKey(), stored.contentType()));
        return mediaMapper.toResponse(media);
    }

    @Transactional(readOnly = true)
    public Media requireBannerMedia(Long id) {
        Media media = mediaRepository.findById(id).orElseThrow(() -> new NotFoundException("Media not found"));
        if (media.getOwnerType() != MediaOwnerType.PROMOTION_BANNER) {
            throw new BadRequestException("This media is not a promotion banner image");
        }
        return media;
    }

    @Transactional(readOnly = true)
    public List<Media> byAdId(Long adId) {
        return mediaRepository.findByAdIdOrderByDisplayOrderAscIdAsc(adId);
    }

    // Batch path for lists of ads: one query total regardless of how many ads are passed in,
    // instead of one media query per ad. Used by read flows that build several AdResponses at
    // once (search results, featured/category-featured carousels, "my ads", etc).
    @Transactional(readOnly = true)
    public Map<Long, List<Media>> byAdIds(Collection<Long> adIds) {
        if (adIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Media>> byAd = new LinkedHashMap<>();
        for (Media media : mediaRepository.findByAdIdInOrderByAdIdAscDisplayOrderAscIdAsc(adIds)) {
            byAd.computeIfAbsent(media.getAd().getId(), k -> new ArrayList<>()).add(media);
        }
        return byAd;
    }
}
