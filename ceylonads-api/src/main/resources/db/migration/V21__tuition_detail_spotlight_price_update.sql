-- Raises the Detail Page Spotlight (TUITION_DETAIL_RIGHT_30D) 30-day base price from Rs. 990 to
-- Rs. 1,490, its permanent normal price outside any campaign. Price only - the plan code, slot
-- mapping, duration, and every other Tuition/MAIN_SITE plan are untouched. The EZCLASS_LAUNCH_990
-- (fixed Rs. 990) and EZCLASS_HALF_PRICE (50% off, Rs. 990 floor) campaigns already resolve
-- dynamically off whatever base price a plan carries (see PromotionPricingService), so this needs
-- no campaign-side change: launch still resolves to 990, and the 50% campaign's raw half (745)
-- still floors to the same 990 minimum.
UPDATE promotion_plans
SET price = 1490.00, updated_at = now()
WHERE code = 'TUITION_DETAIL_RIGHT_30D';
