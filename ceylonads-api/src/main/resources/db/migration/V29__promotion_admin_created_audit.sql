-- Tracks which admin (if any) directly created a promotion via the Tuition admin console's
-- "Promote Class" action (AdminTuitionAdsController), as opposed to a customer's own
-- purchase/request flow. Null for every existing/customer-created promotion.
ALTER TABLE promotions ADD COLUMN created_by_admin_username VARCHAR(255);
