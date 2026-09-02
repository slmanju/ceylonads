package com.slmanju.ceylonads.media.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import com.slmanju.ceylonads.media.dto.StoredMedia;

@Component
// @Profile({ "aws", "dev", "prod" })
public class S3MediaStorage implements MediaStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3MediaStorage(
            S3Client s3Client,
            @Value("${ceylonads.media.s3.bucket}") String bucket,
            @Value("${ceylonads.media.s3.public-base-url}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public StoredMedia store(InputStream input, String originalFilename, String contentType) throws IOException {
        String storageKey = generateStorageKey(originalFilename);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build();

        byte[] bytes = input.readAllBytes();
        s3Client.putObject(request, RequestBody.fromBytes(bytes));

        return new StoredMedia(storageKey, contentType);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build());
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
