-- Replaces EZCLASS_LAUNCH_990 (flat Rs. 990) as the active ezClass launch offer with
-- EZCLASS_LAUNCH_FREE: 100% off (PERCENTAGE_DISCOUNT, minimum_price = 0.00) across all seven
-- current Tuition promotion products, active immediately for 3 months + 1 week from the date this
-- migration is generated (concrete TIMESTAMPTZ values below, not a guessed date and not SQL
-- now() - see PromotionPricingService, which resolves currentPrice = 0 for a 100% discount with no
-- floor above zero).
--
-- EZCLASS_LAUNCH_990 and EZCLASS_HALF_PRICE are deactivated (never deleted) here so
-- EZCLASS_LAUNCH_FREE is the only currently-effective customer-visible TUITION campaign - see
-- PromotionCampaignService#requireNoOverlappingStorefrontCampaign, which would otherwise reject
-- this insert's storefront presentation at the admin-API layer (this migration writes directly to
-- the table, bypassing that service, so the deactivation must happen by hand here). Their pricing,
-- dates, and plan mappings are otherwise untouched, preserving any historical promotion rows sold
-- under them.
UPDATE promotion_campaigns
SET active = false, updated_at = now()
WHERE code IN ('EZCLASS_LAUNCH_990', 'EZCLASS_HALF_PRICE');

INSERT INTO promotion_campaigns (
    code, name, description, source_channel, pricing_type, discount_percent, fixed_price, minimum_price,
    active, starts_at, ends_at, headline, message, cta_label, customer_visible, show_banner, show_modal,
    created_at, updated_at)
SELECT 'EZCLASS_LAUNCH_FREE', 'ezClass Free Launch Promotion',
       '100% off all eligible ezClass promotion placements during our launch period.',
       'TUITION', 'PERCENTAGE_DISCOUNT', 100.00, NULL, 0.00,
       true, '2026-09-03 16:23:27+00', '2026-12-10 16:23:27+00',
       'Promote your class for FREE',
       'All eligible ezClass promotion placements are free during our launch period.',
       'Promote Your Class', true, true, true,
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM promotion_campaigns WHERE code = 'EZCLASS_LAUNCH_FREE');

-- Map the new campaign to all seven current Tuition promotion products (the six from V18 plus
-- Search Page Spotlight restored by V22) - same pattern V18/V22 used for the two campaigns above.
INSERT INTO promotion_campaign_plans (campaign_id, promotion_plan_id)
SELECT c.id, p.id
FROM promotion_campaigns c
CROSS JOIN promotion_plans p
WHERE c.code = 'EZCLASS_LAUNCH_FREE'
  AND p.code IN (
      'TUITION_SEARCH_TOP_30D', 'TUITION_SEARCH_BOOST_30D', 'TUITION_SEARCH_SIDEBAR_TOP_30D',
      'TUITION_HOME_FEATURED_30D', 'TUITION_DETAIL_TOP_30D', 'TUITION_HOME_LATEST_RIGHT_30D',
      'TUITION_DETAIL_RIGHT_30D'
  )
  AND NOT EXISTS (
      SELECT 1 FROM promotion_campaign_plans cp WHERE cp.campaign_id = c.id AND cp.promotion_plan_id = p.id
  );
