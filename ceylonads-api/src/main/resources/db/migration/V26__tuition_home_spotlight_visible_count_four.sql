-- Homepage Spotlight (TUITION_HOME_LATEST_RIGHT) moves from a single-card UI to a vertical
-- carousel showing 4 cards at once. Its capacity (8 concurrent active promotions) was already
-- generous enough for this - only visible_count (the carousel's page size) needs to catch up from
-- 3 to 4 to match the new UI, the same one-column update V25 used for Search Page Spotlight.
-- PromotionSlotService's existing overlap-based capacity math and TuitionFeaturedService's existing
-- size-capped GET /api/tuition/featured retrieval already work generically off these columns, so no
-- application code changes are needed to support this.
UPDATE promotion_slots
SET visible_count = 4,
    updated_at = now()
WHERE code = 'TUITION_HOME_LATEST_RIGHT';
