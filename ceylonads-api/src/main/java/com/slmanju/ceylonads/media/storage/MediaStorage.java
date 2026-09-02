package com.slmanju.ceylonads.media.storage;

import com.slmanju.ceylonads.media.dto.StoredMedia;

import java.io.IOException;
import java.io.InputStream;

public interface MediaStorage {
    StoredMedia store(InputStream input, String originalFilename, String contentType) throws IOException;
    void delete(String storageKey) throws IOException;

    /**
     * Builds the public URL for a stored object from its storage key. Returns null if
     * {@code storageKey} is null/blank rather than producing an invalid URL.
     */
    String publicUrl(String storageKey);
}
