package com.slmanju.ceylonads.media.mapper;

import com.slmanju.ceylonads.media.dto.MediaResponse;
import com.slmanju.ceylonads.media.entity.Media;
import com.slmanju.ceylonads.media.storage.MediaStorage;
import org.springframework.stereotype.Component;

@Component
public class MediaMapper {

    private final MediaStorage storage;

    public MediaMapper(MediaStorage storage) {
        this.storage = storage;
    }

    public MediaResponse toResponse(Media media) {
        return new MediaResponse(media.getId(), storage.publicUrl(media.getStorageKey()), media.getContentType(), media.getDisplayOrder());
    }
}
