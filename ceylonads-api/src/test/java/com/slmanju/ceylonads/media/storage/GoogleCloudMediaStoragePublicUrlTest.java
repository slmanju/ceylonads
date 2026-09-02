package com.slmanju.ceylonads.media.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GoogleCloudMediaStoragePublicUrlTest {

    @Test
    void buildsPublicUrlFromConfiguredBaseUrl() {
        GoogleCloudMediaStorage storage = new GoogleCloudMediaStorage(
                null, "ceylonads-test", "https://storage.googleapis.com/ceylonads-test");

        assertEquals(
                "https://storage.googleapis.com/ceylonads-test/ads/66de0a01-b711-4267-9850-0d110dab2827.jpg",
                storage.publicUrl("ads/66de0a01-b711-4267-9850-0d110dab2827.jpg"));
    }

    @Test
    void trailingSlashOnBaseUrlDoesNotProduceDoubleSlash() {
        GoogleCloudMediaStorage storage = new GoogleCloudMediaStorage(
                null, "ceylonads-test", "https://storage.googleapis.com/ceylonads-test/");

        assertEquals(
                "https://storage.googleapis.com/ceylonads-test/ads/photo.jpg",
                storage.publicUrl("ads/photo.jpg"));
    }

    @Test
    void changingBaseUrlChangesGeneratedUrlForTheSameStorageKey() {
        String storageKey = "ads/photo.jpg";
        GoogleCloudMediaStorage dev = new GoogleCloudMediaStorage(
                null, "ceylonads-dev", "https://storage.googleapis.com/ceylonads-dev");
        GoogleCloudMediaStorage prod = new GoogleCloudMediaStorage(
                null, "ceylonads-prod", "https://storage.googleapis.com/ceylonads-prod");

        assertEquals("https://storage.googleapis.com/ceylonads-dev/ads/photo.jpg", dev.publicUrl(storageKey));
        assertEquals("https://storage.googleapis.com/ceylonads-prod/ads/photo.jpg", prod.publicUrl(storageKey));
    }

    @Test
    void nullStorageKeyReturnsNull() {
        GoogleCloudMediaStorage storage = new GoogleCloudMediaStorage(
                null, "ceylonads-test", "https://storage.googleapis.com/ceylonads-test");

        assertNull(storage.publicUrl(null));
    }

    @Test
    void blankStorageKeyReturnsNull() {
        GoogleCloudMediaStorage storage = new GoogleCloudMediaStorage(
                null, "ceylonads-test", "https://storage.googleapis.com/ceylonads-test");

        assertNull(storage.publicUrl("  "));
    }
}
