-- Cleans up user-facing wording on the two Tuition campaigns seeded by
-- V20__promotion_campaign_launch_offer_presentation.sql. The tuition UI is meant to read as its
-- own ezClass-branded product, so storefront copy should say "class"/"Class" rather than
-- "Tuition Ad"/"My Ad" (see ceylonads-tuition-ui CLAUDE.md "Tuition UX"). Pricing/dates/active
-- flags are untouched here - this migration only updates presentation copy.
UPDATE promotion_campaigns
SET headline = 'Promote your class for just Rs. 990',
    cta_label = 'Promote My Class',
    updated_at = now()
WHERE code = 'EZCLASS_LAUNCH_990';

UPDATE promotion_campaigns
SET headline = 'Get 50% Off Class Promotions',
    cta_label = 'Promote My Class',
    updated_at = now()
WHERE code = 'EZCLASS_HALF_PRICE';
