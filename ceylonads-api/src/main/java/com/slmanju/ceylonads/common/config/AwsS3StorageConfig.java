package com.slmanju.ceylonads.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
// @Profile({ "aws", "dev", "prod" })
public class AwsS3StorageConfig {

    @Bean
    public S3Client s3Client(@Value("${ceylonads.media.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
