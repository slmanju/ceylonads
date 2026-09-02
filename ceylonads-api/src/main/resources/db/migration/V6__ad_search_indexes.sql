-- Ad search performance indexes, added after reviewing the actual query patterns used by
-- AdSearchService/AdSpecifications against the schema created in V1.
--
-- ads: the public search/browse path always filters status = 'ACTIVE', and very often also
-- filters category_id (a category page, or the resolved category subtree), then sorts by
-- created_at DESC for pagination. idx_ads_status_created (status, created_at) already covers the
-- no-category case; it cannot also satisfy an added category_id predicate without an extra sort
-- step. category_id leads this new index (rather than status) so it fully subsumes the existing
-- single-column idx_ads_category for any category-scoped lookup, not just the current one.
CREATE INDEX idx_ads_category_status_created ON ads (category_id, status, created_at);

-- media: every ad-detail view and every page of search results batch-loads media rows by ad_id
-- (ordered by display_order), and media had no index on ad_id at all - every one of those lookups
-- was a full table scan. This directly supports MediaRepository.findByAdIdOrderByDisplayOrderAscIdAsc,
-- findByAdIdInOrderByAdIdAscDisplayOrderAscIdAsc, and countByAdId.
CREATE INDEX idx_media_ad_display ON media (ad_id, display_order);
