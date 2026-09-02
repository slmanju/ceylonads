package com.slmanju.ceylonads.common.util;

import java.util.regex.Pattern;

public final class Slugs {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-+)|(-+$)");
    private static final Pattern TRAILING_DIGITS = Pattern.compile("(\\d+)$");

    private Slugs() {
    }

    // Not persisted: an ad's public slug is derived on read from its title and id, so it never
    // needs a migration or a uniqueness check - the trailing "-{id}" already guarantees uniqueness.
    public static String slugify(String value) {
        String normalized = NON_ALPHANUMERIC.matcher(value.toLowerCase()).replaceAll("-");
        String trimmed = EDGE_HYPHENS.matcher(normalized).replaceAll("");
        return trimmed.isEmpty() ? "item" : trimmed;
    }

    public static String adSlug(String title, Long id) {
        return slugify(title) + "-" + id;
    }

    // Accepts either a bare numeric id ("12345") or a full slug ("toyota-aqua-2019-12345") and
    // recovers the numeric id from the end - the id is always the last hyphen-delimited token.
    public static Long extractTrailingId(String idOrSlug) {
        if (idOrSlug == null) return null;
        var matcher = TRAILING_DIGITS.matcher(idOrSlug.trim());
        if (!matcher.find()) return null;
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
