package com.example.s3webapp.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3")
public record S3Properties(List<BucketConfig> buckets) {

    public S3Properties {
        buckets = buckets == null ? List.of() : buckets;
    }

    public record BucketConfig(
            @NotBlank String id,
            @NotBlank String name,
            @NotBlank String bucketName,
            @NotBlank String endpointUrl,
            @NotBlank String accessKey,
            @NotBlank String secretKey,
            @NotBlank String region,
            @NotNull Boolean pathStyleAccess) {}
}
