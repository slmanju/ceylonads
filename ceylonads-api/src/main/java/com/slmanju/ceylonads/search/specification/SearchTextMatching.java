package com.slmanju.ceylonads.search.specification;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdAttributeValue;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.List;

/**
 * Shared word-boundary matching used by both the keyword-search inclusion filter
 * (AdKeywordSpecifications) and the relevance ranking (AdRelevanceOrdering), so a query like
 * "tea" matches only against title/description/category-name/searchable-attribute text that
 * contains "tea" as its own word - never as a bare substring inside an unrelated word such as
 * "teacher".
 */
final class SearchTextMatching {

    private static final char ESCAPE_CHAR = '\\';

    private SearchTextMatching() {
    }

    static String normalize(String q) {
        if (q == null) return null;
        String collapsed = q.trim().toLowerCase().replaceAll("\\s+", " ");
        return collapsed.isEmpty() ? null : collapsed;
    }

    static List<String> tokens(String normalizedQuery) {
        if (normalizedQuery == null) return List.of();
        return List.of(normalizedQuery.split(" "));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    static Predicate startsWith(CriteriaBuilder cb, Expression<String> loweredField, String value) {
        return cb.like(loweredField, escapeLike(value) + "%", ESCAPE_CHAR);
    }

    static Predicate contains(CriteriaBuilder cb, Expression<String> loweredField, String value) {
        return cb.like(loweredField, "%" + escapeLike(value) + "%", ESCAPE_CHAR);
    }

    // Matches value only as a standalone, space-delimited word/phrase (the whole field, or at its
    // start/end/middle) - not as a fragment inside a longer word, e.g. "tea" matches "fresh tea"
    // but not "teacher".
    static Predicate wholeWordLike(CriteriaBuilder cb, Expression<String> loweredField, String value) {
        String escaped = escapeLike(value);
        return cb.or(
                cb.like(loweredField, escaped, ESCAPE_CHAR),
                cb.like(loweredField, escaped + " %", ESCAPE_CHAR),
                cb.like(loweredField, "% " + escaped, ESCAPE_CHAR),
                cb.like(loweredField, "% " + escaped + " %", ESCAPE_CHAR));
    }

    // Correlated EXISTS against AdAttributeValue, mirroring AdAttributeSpecifications: only
    // attribute definitions explicitly marked searchable participate in free-text matching, and an
    // ad with many attribute rows never gets joined/duplicated.
    static Predicate searchableAttributeMatches(
            Root<Ad> root, CriteriaQuery<?> query, CriteriaBuilder cb, String value, boolean requireWholeWord) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<AdAttributeValue> av = subquery.from(AdAttributeValue.class);
        subquery.select(av.get("id"));

        Expression<String> loweredValueText = cb.lower(av.get("valueText"));
        Predicate valueMatch = requireWholeWord
                ? wholeWordLike(cb, loweredValueText, value)
                : contains(cb, loweredValueText, value);

        subquery.where(cb.and(
                cb.equal(av.get("ad"), root),
                cb.isTrue(av.get("attributeDefinition").get("searchable")),
                valueMatch));
        return cb.exists(subquery);
    }
}
