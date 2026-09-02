package com.slmanju.ceylonads.ad.specification;

import com.slmanju.ceylonads.ad.entity.Ad;
import com.slmanju.ceylonads.ad.entity.AdAttributeValue;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * EXISTS-subquery filters against AdAttributeValue, mirroring the shape of
 * promotion/specification/AdPromotionSpecifications - each filter is its own correlated subquery so
 * an ad with many attribute rows never gets joined/duplicated, and the resulting predicate composes
 * cleanly with the rest of the search baseSpec (category, location, price, promotion).
 */
public final class AdAttributeSpecifications {

    private AdAttributeSpecifications() {
    }

    // Exact-match filters don't know the attribute's dataType ahead of time (the search layer only
    // has a raw query string), so this matches whichever typed column the value could plausibly
    // represent: text/SELECT via valueText, or - when the value happens to parse as a number/
    // boolean - valueNumber/valueBoolean too. A NUMBER attribute's values only ever live in
    // valueNumber, so without this an "attr.year=2019" filter would silently match nothing.
    public static Specification<Ad> hasAttributeValue(String key, String value) {
        BigDecimal numericValue = tryParseNumber(value);
        Boolean booleanValue = tryParseBoolean(value);

        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<AdAttributeValue> av = subquery.from(AdAttributeValue.class);
            subquery.select(av.get("id"));

            Predicate valueMatch = cb.equal(cb.lower(av.get("valueText")), value.toLowerCase());
            if (numericValue != null) {
                valueMatch = cb.or(valueMatch, cb.equal(av.get("valueNumber"), numericValue));
            }
            if (booleanValue != null) {
                valueMatch = cb.or(valueMatch, cb.equal(av.get("valueBoolean"), booleanValue));
            }

            subquery.where(cb.and(
                    cb.equal(av.get("ad"), root),
                    cb.equal(av.get("attributeDefinition").get("key"), key),
                    valueMatch));
            return cb.exists(subquery);
        };
    }

    public static Specification<Ad> hasAttributeNumberInRange(String key, BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<AdAttributeValue> av = subquery.from(AdAttributeValue.class);
            subquery.select(av.get("id"));

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(av.get("ad"), root));
            predicates.add(cb.equal(av.get("attributeDefinition").get("key"), key));
            if (min != null) predicates.add(cb.greaterThanOrEqualTo(av.get("valueNumber"), min));
            if (max != null) predicates.add(cb.lessThanOrEqualTo(av.get("valueNumber"), max));
            subquery.where(cb.and(predicates.toArray(new Predicate[0])));

            return cb.exists(subquery);
        };
    }

    private static BigDecimal tryParseNumber(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean tryParseBoolean(String value) {
        if (value.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (value.equalsIgnoreCase("false")) return Boolean.FALSE;
        return null;
    }
}
