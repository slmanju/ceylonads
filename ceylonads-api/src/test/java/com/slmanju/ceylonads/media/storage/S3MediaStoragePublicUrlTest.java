package com.slmanju.ceylonads.media.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class S3MediaStoragePublicUrlTest {

    @Test
    void buildsPublicUrlFromConfiguredBaseUrl() {
        S3MediaStorage storage = new S3MediaStorage(
                null, "ceylonads-test", "https://ceylonads-test.s3.amazonaws.com");

        assertEquals(
                "https://ceylonads-test.s3.amazonaws.com/ads/66de0a01-b711-4267-9850-0d110dab2827.jpg",
                storage.publicUrl("ads/66de0a01-b711-4267-9850-0d110dab2827.jpg"));
    }

    @Test
    void trailingSlashOnBaseUrlDoesNotProduceDoubleSlash() {
        S3MediaStorage storage = new S3MediaStorage(
                null, "ceylonads-test", "https://ceylonads-test.s3.amazonaws.com/");

        assertEquals(
                "https://ceylonads-test.s3.amazonaws.com/ads/photo.jpg",
                storage.publicUrl("ads/photo.jpg"));
    }

    @Test
    void changingBaseUrlChangesGeneratedUrlForTheSameStorageKey() {
        String storageKey = "ads/photo.jpg";
        S3MediaStorage dev = new S3MediaStorage(
                null, "ceylonads-dev", "https://ceylonads-dev.s3.amazonaws.com");
        S3MediaStorage prod = new S3MediaStorage(
                null, "ceylonads-prod", "https://ceylonads-prod.s3.amazonaws.com");

        assertEquals("https://ceylonads-dev.s3.amazonaws.com/ads/photo.jpg", dev.publicUrl(storageKey));
        assertEquals("https://ceylonads-prod.s3.amazonaws.com/ads/photo.jpg", prod.publicUrl(storageKey));
    }

    @Test
    void nullStorageKeyReturnsNull() {
        S3MediaStorage storage = new S3MediaStorage(
                null, "ceylonads-test", "https://ceylonads-test.s3.amazonaws.com");

        assertNull(storage.publicUrl(null));
    }

    @Test
    void blankStorageKeyReturnsNull() {
        S3MediaStorage storage = new S3MediaStorage(
                null, "ceylonads-test", "https://ceylonads-test.s3.amazonaws.com");

        assertNull(storage.publicUrl("  "));
    }
}
