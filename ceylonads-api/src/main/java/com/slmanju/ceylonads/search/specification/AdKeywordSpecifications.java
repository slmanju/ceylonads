package com.slmanju.ceylonads.search.specification;

import com.slmanju.ceylonads.ad.entity.Ad;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Global free-text search inclusion filter. Every token in q must appear as a standalone word
 * somewhere across title, description, category name, or a searchable attribute value (AND
 * across tokens, OR across fields per token) - so "toyota corolla" favors ads containing both
 * terms, and a single token like "tea" can't match through a bare substring inside an unrelated
 * word like "teacher".
 */
public final class AdKeywordSpecifications {

    private AdKeywordSpecifications() {
    }

    public static Specification<Ad> matches(String q) {
        String normalized = SearchTextMatching.normalize(q);
        if (normalized == null) return null;
        List<String> tokens = SearchTextMatching.tokens(normalized);

        return (root, query, cb) -> {
            List<Predicate> tokenPredicates = new ArrayList<>();
            for (String token : tokens) {
                tokenPredicates.add(cb.or(
                        SearchTextMatching.wholeWordLike(cb, cb.lower(root.get("title")), token),
                        SearchTextMatching.wholeWordLike(cb, cb.lower(root.get("description")), token),
                        SearchTextMatching.wholeWordLike(cb, cb.lower(root.get("category").get("name")), token),
                        SearchTextMatching.searchableAttributeMatches(root, query, cb, token, true)));
            }
            return cb.and(tokenPredicates.toArray(new Predicate[0]));
        };
    }
}
