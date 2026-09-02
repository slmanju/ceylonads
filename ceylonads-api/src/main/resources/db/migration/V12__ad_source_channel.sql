-- Introduces source_channel on ads: which storefront/vertical owns a listing (MAIN_SITE, TUITION,
-- BOARDING), as opposed to category (what the listing is about). Only the MAIN CeylonAds
-- storefront is in production today, so every existing production ad is safely classified
-- MAIN_SITE here. DEV's ~1,000-ad Tuition performance dataset and the hand-written Tuition sample
-- ads are reclassified separately by the DEV sample-data seeders (SampleDataSeeder,
-- TuitionPerformanceSeeder), not by this migration - this migration only ever needs to know about
-- production, where nothing but MAIN_SITE exists yet.
ALTER TABLE ads ADD COLUMN source_channel character varying(20);
UPDATE ads SET source_channel = 'MAIN_SITE';
ALTER TABLE ads ALTER COLUMN source_channel SET NOT NULL;
ALTER TABLE ads ADD CONSTRAINT ck_ads_source_channel CHECK (source_channel IN ('MAIN_SITE', 'TUITION', 'BOARDING'));

-- Backs the MAIN public marketplace's default browse/keyword search (source_channel = 'MAIN_SITE'
-- AND status = 'ACTIVE', ordered by created_at DESC) once that filter is added at the search
-- boundary. Category-scoped browsing keeps using idx_ads_category_status_created - source_channel
-- isn't very selective within one category today, and MAIN_SITE is currently ~100% of production
-- rows, so this index earns its keep on the no-category path, not by replacing the category index.
CREATE INDEX idx_ads_source_channel_status_created ON ads (source_channel, status, created_at);
