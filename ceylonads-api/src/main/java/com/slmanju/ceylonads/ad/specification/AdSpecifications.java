package com.slmanju.ceylonads.ad.specification;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdLocation;
import com.slmanju.ceylonads.ad.entity.AdStatus;
import com.slmanju.ceylonads.ad.entity.SourceChannel;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collection;

public final class AdSpecifications {

    private AdSpecifications() {
    }

    public static Specification<Ad> active() {
        return (root, query, cb) -> cb.equal(root.get("status"), AdStatus.ACTIVE);
    }

    // MAIN public marketplace boundary: only used by AdSearchService, never by admin/moderation/
    // seller queries, which stay channel-agnostic.
    public static Specification<Ad> sourceChannel(SourceChannel channel) {
        return (root, query, cb) -> cb.equal(root.get("sourceChannel"), channel);
    }

    // ids is the resolved category (self + descendants) so a parent-category filter matches every
    // subcategory's ads without needing a slug-based join.
    public static Specification<Ad> categoryIdIn(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return (root, query, cb) -> root.get("category").get("id").in(ids);
    }

    // ids is the resolved location (self + descendants), same reasoning as categoryIdIn. An ad
    // matches if ANY of its 0..N locations is in the set - a correlated EXISTS against the
    // AdLocation join rows, mirroring AdAttributeSpecifications, so an ad with several locations
    // never gets joined/duplicated by this filter.
    public static Specification<Ad> locationIdIn(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<AdLocation> al = subquery.from(AdLocation.class);
            subquery.select(al.get("id"));
            subquery.where(cb.and(
                    cb.equal(al.get("ad"), root),
                    al.get("location").get("id").in(ids)));
            return cb.exists(subquery);
        };
    }

    public static Specification<Ad> minPrice(BigDecimal value) {
        if (value == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), value);
    }

    public static Specification<Ad> maxPrice(BigDecimal value) {
        if (value == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), value);
    }

    // Used to keep promoted ads beyond a placement's visibleCount in the normal ranked pool
    // (instead of dropping them) once they're no longer part of the boosted subset.
    public static Specification<Ad> excludingIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return (root, query, cb) -> cb.not(root.get("id").in(ids));
    }
}
