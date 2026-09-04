package com.slmanju.ceylonads.tuition.service;

import com.slmanju.ceylonads.ad.entity.SourceChannel;
import com.slmanju.ceylonads.ad.service.AdService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Eventual persisted-status cleanup for stale Tuition listings: hourly, flips every ACTIVE TUITION
 * ad whose expiresAt has passed to EXPIRED (see AdRepository#expireOverdue - a single bulk update,
 * never loading rows into memory). Deliberately scoped to TUITION only, via a real parameter, not a
 * hardcoded query - MAIN_SITE/BOARDING ads are never touched.
 *
 * <p>This is only ever the second of two layers: every public Tuition read path (search, detail,
 * similar, latest, featured/promoted carousels) already checks expiresAt directly (see
 * AdSpecifications#notExpired and the Tuition repositories' own expiresAt guards), so an ad becomes
 * publicly invisible immediately at its deadline regardless of when this next runs.
 */
@Component
public class TuitionExpiryScheduler {

    private final AdService adService;

    public TuitionExpiryScheduler(AdService adService) {
        this.adService = adService;
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void expireOverdueTuitionListings() {
        adService.expireOverdue(SourceChannel.TUITION);
    }
}
