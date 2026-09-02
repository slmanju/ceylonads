package com.slmanju.ceylonads.media.mapper;

import com.slmanju.ceylonads.media.dto.MediaResponse;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.storage.MediaStorage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediaMapperTest {

    @Test
    void urlIsResolvedFromStorageKeyAtMappingTime() {
        Media media = Media.forPromotionBanner("ads/photo.jpg", "image/jpeg");

        MediaStorage storage = mock(MediaStorage.class);
        when(storage.publicUrl("ads/photo.jpg")).thenReturn("https://cdn.example.com/ads/photo.jpg");

        MediaResponse response = new MediaMapper(storage).toResponse(media);

        assertEquals("https://cdn.example.com/ads/photo.jpg", response.url());
        assertEquals("ads/photo.jpg", media.getStorageKey());
    }

    // Same persisted Media row, two different storage configurations: the generated URL changes
    // but the entity's storageKey - the only thing actually persisted - never does.
    @Test
    void changingConfiguredBaseUrlChangesResponseWithoutChangingTheMediaRow() {
        Media media = Media.forPromotionBanner("ads/photo.jpg", "image/jpeg");

        MediaStorage oldEnvironment = mock(MediaStorage.class);
        when(oldEnvironment.publicUrl("ads/photo.jpg")).thenReturn("https://old-bucket.example.com/ads/photo.jpg");
        MediaStorage newEnvironment = mock(MediaStorage.class);
        when(newEnvironment.publicUrl("ads/photo.jpg")).thenReturn("https://new-bucket.example.com/ads/photo.jpg");

        String oldUrl = new MediaMapper(oldEnvironment).toResponse(media).url();
        String newUrl = new MediaMapper(newEnvironment).toResponse(media).url();

        assertNotEquals(oldUrl, newUrl);
        assertEquals("ads/photo.jpg", media.getStorageKey());
    }
}
