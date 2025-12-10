package com.example.s3webapp.s3;

import com.example.s3webapp.config.S3Properties.BucketConfig;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Component
public class S3ClientFactory {

    private final Map<String, S3Client> clients = new ConcurrentHashMap<>();

    public S3Client clientFor(BucketConfig config) {
        return clients.computeIfAbsent(config.id(), id -> buildClient(config));
    }

    private S3Client buildClient(BucketConfig config) {
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(Boolean.TRUE.equals(config.pathStyleAccess()))
                .build();
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKey(), config.secretKey())))
                .endpointOverride(URI.create(config.endpointUrl()))
                .region(Region.of(config.region()))
                .serviceConfiguration(s3Configuration)
                .build();
    }
}
