-- Search Page Spotlight (TUITION_SEARCH_SIDEBAR_TOP) moves from a single-advertiser slot to a
-- 12-advertiser vertical carousel: up to 12 concurrent active promotions (capacity), 4 of which
-- render to a visitor at once (visible_count - the carousel's page size). Still one plan, one slot,
-- one price (see TUITION_SEARCH_SIDEBAR_TOP_30D, unchanged) - only these two slot numbers change.
-- PromotionSlotService's existing overlap-based capacity math (countOverlapping vs. capacity) and
-- TuitionFeaturedService's existing size-capped GET /api/tuition/featured retrieval already work
-- generically off these columns, so no application code changes are needed to support this.
UPDATE promotion_slots
SET capacity = 12,
    visible_count = 4,
    updated_at = now()
WHERE code = 'TUITION_SEARCH_SIDEBAR_TOP';
