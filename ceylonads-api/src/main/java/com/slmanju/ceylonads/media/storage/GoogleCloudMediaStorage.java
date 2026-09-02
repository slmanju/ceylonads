package com.slmanju.ceylonads.media.storage;

import org.springframework.context.annotation.Profile;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import com.slmanju.ceylonads.media.dto.StoredMedia;

@Component
@Profile({"gcloud"})
public class GoogleCloudMediaStorage implements MediaStorage {

    private final Storage storage;
    private final String bucket;
    private final String publicBaseUrl;

    public GoogleCloudMediaStorage(
            Storage storage,
            @Value("${ceylonads.media.gcs.bucket}") String bucket,
            @Value("${ceylonads.media.gcs.public-base-url}") String publicBaseUrl) {
        this.storage = storage;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public StoredMedia store(InputStream input, String originalFilename, String contentType) throws IOException {
        String storageKey = generateStorageKey(originalFilename);
        BlobId blobId = BlobId.of(bucket, storageKey);

        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .setCacheControl("public, max-age=31536000, immutable")
                .build();

        byte[] bytes = input.readAllBytes();
        storage.create(blobInfo, bytes);

        return new StoredMedia(storageKey, contentType);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        storage.delete(BlobId.of(bucket, storageKey));
    }

    @Override
    public String publicUrl(String storageKey) {
        return PublicUrls.join(publicBaseUrl, storageKey);
    }

    private String generateStorageKey(String originalFilename) {
        String extension = extractExtension(originalFilename);
        String filename = UUID.randomUUID().toString();

        if (!extension.isBlank()) {
            filename += "." + extension;
        }

        return "ads/" + filename;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }

        int index = originalFilename.lastIndexOf('.');

        if (index < 0
                || index == originalFilename.length() - 1) {
            return "";
        }

        return originalFilename.substring(index + 1).toLowerCase();
    }
}