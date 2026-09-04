package com.slmanju.ceylonads.promotion.specification;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.promotion.entity.PlacementType;
import com.slmanju.ceylonads.promotion.entity.Promotion;
import com.slmanju.ceylonads.promotion.entity.PromotionStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * Ad-side specifications that boost/exclude by an EXISTS check against Promotion, so ranking
 * queries stay proper WHERE-clause filters (no "load everything, sort in Java" for the bulk of
 * the catalog).
 */
public final class AdPromotionSpecifications {

    private AdPromotionSpecifications() {
    }

    public static Specification<Ad> hasActivePromotion(PlacementType placementType) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Promotion> promotion = subquery.from(Promotion.class);
            subquery.select(promotion.get("id"));
            subquery.where(cb.and(
                    cb.equal(promotion.get("ad"), root),
                    cb.equal(promotion.get("status"), PromotionStatus.ACTIVE),
                    cb.greaterThan(promotion.get("endsAt"), Instant.now()),
                    cb.equal(promotion.get("plan").get("slot").get("placementType"), placementType)));

            return cb.exists(subquery);
        };
    }

    // Exact-slot-code variant of hasActivePromotion(PlacementType) above, for placements (e.g.
    // Tuition's TUITION_SEARCH_BOOST) whose placementType is shared with other, unrelated slots -
    // see AdSearchService's Tuition Search Boost overload for why placementType alone isn't
    // specific enough there.
    public static Specification<Ad> hasActivePromotionSlotCode(String slotCode) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Promotion> promotion = subquery.from(Promotion.class);
            subquery.select(promotion.get("id"));
            subquery.where(cb.and(
                    cb.equal(promotion.get("ad"), root),
                    cb.equal(promotion.get("status"), PromotionStatus.ACTIVE),
                    cb.greaterThan(promotion.get("endsAt"), Instant.now()),
                    cb.equal(promotion.get("plan").get("slot").get("code"), slotCode)));

            return cb.exists(subquery);
        };
    }
}
