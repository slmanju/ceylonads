package com.slmanju.ceylonads.search.specification;

import com.slmanju.ceylonads.ad.entity.Ad;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

/**
 * Deterministic relevance ordering for keyword search, expressed as a single Criteria CASE score
 * plus createdAt as the tiebreaker. Buckets are (highest first): exact title match, title starts
 * with query, title contains query as a whole word/phrase, title contains query anywhere,
 * category/subcategory name match, description match, searchable attribute-value match. With no
 * query text, every row scores the same and the ordering reduces to plain createdAt DESC.
 */
public final class AdRelevanceOrdering {

    private AdRelevanceOrdering() {
    }

    public static List<Order> apply(Root<Ad> root, CriteriaQuery<?> query, CriteriaBuilder cb, String q) {
        String normalized = SearchTextMatching.normalize(q);
        Order newestFirst = cb.desc(root.get("createdAt"));
        if (normalized == null) {
            return List.of(newestFirst);
        }
        return List.of(cb.desc(relevanceScore(root, query, cb, normalized)), newestFirst);
    }

    private static Expression<Integer> relevanceScore(
            Root<Ad> root, CriteriaQuery<?> query, CriteriaBuilder cb, String normalized) {
        Expression<String> title = cb.lower(root.get("title"));
        Expression<String> description = cb.lower(root.get("description"));
        Expression<String> categoryName = cb.lower(root.get("category").get("name"));

        Predicate titleExact = cb.equal(title, normalized);
        Predicate titleStarts = SearchTextMatching.startsWith(cb, title, normalized);
        Predicate titleWholeWord = SearchTextMatching.wholeWordLike(cb, title, normalized);
        Predicate titleContains = SearchTextMatching.contains(cb, title, normalized);
        Predicate categoryContains = SearchTextMatching.contains(cb, categoryName, normalized);
        Predicate descriptionContains = SearchTextMatching.contains(cb, description, normalized);
        Predicate attributeMatch = SearchTextMatching.searchableAttributeMatches(root, query, cb, normalized, false);

        return cb.<Integer>selectCase()
                .when(titleExact, 100)
                .when(titleStarts, 90)
                .when(titleWholeWord, 80)
                .when(titleContains, 70)
                .when(categoryContains, 60)
                .when(descriptionContains, 50)
                .when(attributeMatch, 40)
                .otherwise(0);
    }
}
