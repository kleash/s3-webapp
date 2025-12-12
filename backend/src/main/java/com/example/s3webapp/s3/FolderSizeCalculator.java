package com.example.s3webapp.s3;

import com.example.s3webapp.config.S3Properties.BucketConfig;
import com.example.s3webapp.util.KeyUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@Component
public class FolderSizeCalculator {

    private final BucketRegistry bucketRegistry;
    private final S3ClientFactory s3ClientFactory;

    public FolderSizeCalculator(BucketRegistry bucketRegistry, S3ClientFactory s3ClientFactory) {
        this.bucketRegistry = bucketRegistry;
        this.s3ClientFactory = s3ClientFactory;
    }

    public FolderSizeComputation compute(
            String bucketId,
            String prefix,
            FolderSizeLimits limits,
            BooleanSupplier cancelRequested,
            Consumer<FolderSizeComputation> progressConsumer,
            int progressPageInterval) {
        String normalizedPrefix = KeyUtils.normalizePrefix(prefix);
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);
        long total = 0;
        long count = 0;
        boolean partial = false;
        String partialReason = null;
        String token = null;
        Instant started = Instant.now();
        int page = 0;

        do {
            checkCancelled(cancelRequested);
            if (limits.hasObjectCap() && count >= limits.maxObjects()) {
                partial = true;
                partialReason = "max-objects";
                break;
            }
            if (limits.hasRuntimeCap() && Duration.between(started, Instant.now()).compareTo(limits.maxRuntime()) > 0) {
                partial = true;
                partialReason = "max-runtime";
                break;
            }

            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(config.bucketName())
                    .prefix(normalizedPrefix)
                    .maxKeys(500);
            if (token != null) builder.continuationToken(token);
            ListObjectsV2Response response = client.listObjectsV2(builder.build());
            for (S3Object obj : response.contents()) {
                if (obj.key().endsWith("/")) continue;
                total += obj.size();
                count++;
                if (limits.hasObjectCap() && count >= limits.maxObjects()) {
                    partial = true;
                    partialReason = "max-objects";
                    break;
                }
                checkCancelled(cancelRequested);
                if (limits.hasRuntimeCap()
                        && Duration.between(started, Instant.now()).compareTo(limits.maxRuntime()) > 0) {
                    partial = true;
                    partialReason = "max-runtime";
                    break;
                }
            }
            token = response.nextContinuationToken();
            page++;
            if (progressConsumer != null
                    && progressPageInterval > 0
                    && (token == null || page % progressPageInterval == 0)) {
                progressConsumer.accept(
                        new FolderSizeComputation(normalizedPrefix, count, total, partial, partialReason, false));
            }
        } while (token != null && !partial);

        FolderSizeComputation result =
                new FolderSizeComputation(normalizedPrefix, count, total, partial, partialReason, true);
        if (progressConsumer != null) {
            progressConsumer.accept(result);
        }
        return result;
    }

    private void checkCancelled(BooleanSupplier cancelRequested) {
        if (cancelRequested != null && cancelRequested.getAsBoolean()) {
            throw new FolderSizeCancelledException("Folder size calculation cancelled");
        }
    }
}
