-- Introduces source_channel on promotion_slots: which storefront/vertical a slot's inventory
-- belongs to (MAIN_SITE, TUITION, BOARDING), mirroring ads.source_channel (see
-- V12__ad_source_channel.sql). Without this, PromotionService.compatiblePlansForAd had no way to
-- tell a channel-agnostic slot (e.g. HOME_FEATURED, TOP_SEARCH, AD_DETAIL_SIDEBAR - all have no
-- category, so they were treated as compatible with any ad) apart from a channel-specific one, so
-- generic CeylonAds plans were leaking into the Tuition promotion-purchase catalog. Every existing
-- slot predates the Tuition/BOARDING channels and was sold on the main CeylonAds storefront, so
-- backfilling MAIN_SITE here is safe and matches V12's precedent exactly.
ALTER TABLE promotion_slots ADD COLUMN source_channel character varying(20);
UPDATE promotion_slots SET source_channel = 'MAIN_SITE';
ALTER TABLE promotion_slots ALTER COLUMN source_channel SET NOT NULL;
ALTER TABLE promotion_slots ADD CONSTRAINT ck_promotion_slots_source_channel CHECK (source_channel IN ('MAIN_SITE', 'TUITION', 'BOARDING'));
