package com.slmanju.ceylonads.media.storage;

/**
 * Joins a configured base URL/prefix with a storage key without producing a doubled slash,
 * regardless of whether the base already ends in "/" or the key already starts with one.
 */
final class PublicUrls {

    private PublicUrls() {
    }

    static String join(String base, String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        String trimmedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String trimmedKey = storageKey.startsWith("/") ? storageKey.substring(1) : storageKey;
        return trimmedBase + "/" + trimmedKey;
    }
}
