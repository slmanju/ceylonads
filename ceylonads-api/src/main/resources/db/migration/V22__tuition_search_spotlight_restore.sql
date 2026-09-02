-- Restores the seventh Tuition promotion product, "Search Page Spotlight" - the fixed right-side
-- placement beside the Tuition search results - which V18's six-product cleanup incorrectly
-- dropped from the catalog entirely. Reuses the stable, historically-used right-side slot
-- (TUITION_SEARCH_SIDEBAR_TOP, capacity 1 / visibleCount 1 - see V11) rather than inventing a new
-- slot code: only its user-facing name/description change, its code/category/placement_type are
-- untouched, so any already-sold TUITION_SEARCH_SIDEBAR_TOP_7D promotion keeps resolving exactly
-- as before. TUITION_SEARCH_SIDEBAR_MIDDLE/BOTTOM (never part of the final search-page UI - only
-- one fixed right-side position exists) stay exactly as V18 left them: inactive, unexposed, rows
-- preserved for any historical reference.

-- 1) Reactivate the slot and rename it for the current catalog. code/placement_type/category are
--    immutable columns by convention elsewhere in this schema (see PromotionSlot entity) - only
--    name/description/active/updated_at change here.
UPDATE promotion_slots
SET name = 'Search Page Spotlight',
    description = 'Appear beside Tuition search results for high-visibility exposure.',
    source_channel = 'TUITION',
    active = true,
    updated_at = now()
WHERE code = 'TUITION_SEARCH_SIDEBAR_TOP';

-- 2) A new 30-day plan for it, following the same "<SLOT>_30D" convention as the other six
--    products (see V18). The old TUITION_SEARCH_SIDEBAR_TOP_7D plan is a distinct, separate row -
--    left inactive (V18 already deactivated it) rather than reactivated/reused, since its 7-day
--    duration and legacy price no longer match the current catalog.
INSERT INTO promotion_plans (code, name, description, promotion_slot_id, duration_days, price, payment_required, approval_required, display_order, active, created_at, updated_at)
SELECT 'TUITION_SEARCH_SIDEBAR_TOP_30D', 'Search Page Spotlight', 'Appear beside Tuition search results for high-visibility exposure.',
       (SELECT id FROM promotion_slots WHERE code = 'TUITION_SEARCH_SIDEBAR_TOP'), 30, 2490.00, true, true, 25, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_plans WHERE code = 'TUITION_SEARCH_SIDEBAR_TOP_30D');

-- 3) Map the new plan into both existing launch campaigns, same as V18 did for the original six -
--    PromotionPricingService resolves currentPrice dynamically off this mapping and the plan's
--    base price above, so no price is duplicated/hardcoded here: EZCLASS_LAUNCH_990 (fixed Rs. 990)
--    and EZCLASS_HALF_PRICE (50% off, Rs. 990 floor -> 1245.00) both apply automatically once mapped.
INSERT INTO promotion_campaign_plans (campaign_id, promotion_plan_id)
SELECT c.id, p.id
FROM promotion_campaigns c
CROSS JOIN promotion_plans p
WHERE c.code IN ('EZCLASS_LAUNCH_990', 'EZCLASS_HALF_PRICE')
  AND p.code = 'TUITION_SEARCH_SIDEBAR_TOP_30D'
  AND NOT EXISTS (
      SELECT 1 FROM promotion_campaign_plans cp WHERE cp.campaign_id = c.id AND cp.promotion_plan_id = p.id
  );
