package com.example.s3webapp.s3;

import com.example.s3webapp.config.S3Properties.BucketConfig;
import com.example.s3webapp.model.CopyMoveRequest;
import com.example.s3webapp.model.DeleteFolderRequest;
import com.example.s3webapp.model.DeleteObjectsRequest;
import com.example.s3webapp.model.FolderItem;
import com.example.s3webapp.model.FolderSizeResponse;
import com.example.s3webapp.model.BulkCopyMoveRequest;
import com.example.s3webapp.model.BulkOperationResult;
import com.example.s3webapp.model.FolderCopyRequest;
import com.example.s3webapp.model.FolderOperationResult;
import com.example.s3webapp.model.ObjectItem;
import com.example.s3webapp.model.ObjectListResponse;
import com.example.s3webapp.util.KeyUtils;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class StorageService {

    private final BucketRegistry bucketRegistry;
    private final S3ClientFactory s3ClientFactory;

    public StorageService(BucketRegistry bucketRegistry, S3ClientFactory s3ClientFactory) {
        this.bucketRegistry = bucketRegistry;
        this.s3ClientFactory = s3ClientFactory;
    }

    public List<BucketConfig> listBuckets() {
        return bucketRegistry.list();
    }

    public ObjectListResponse listObjects(String bucketId, String prefix, String continuationToken) {
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);

        String normalizedPrefix = KeyUtils.normalizePrefix(prefix);
        ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(config.bucketName())
                .prefix(normalizedPrefix)
                .delimiter("/")
                .maxKeys(500);
        if (continuationToken != null && !continuationToken.isBlank()) {
            requestBuilder.continuationToken(continuationToken);
        }

        ListObjectsV2Response response = client.listObjectsV2(requestBuilder.build());
        List<FolderItem> folders = response.commonPrefixes().stream()
                .map(cp -> new FolderItem(KeyUtils.folderNameFromPrefix(normalizedPrefix, cp.prefix()), cp.prefix()))
                .toList();

        List<ObjectItem> objects = response.contents().stream()
                .filter(o -> !o.key().endsWith("/"))
                .map(o -> toObjectItem(client, config.bucketName(), o))
                .toList();

        return new ObjectListResponse(normalizedPrefix, folders, objects, response.nextContinuationToken());
    }

    public ObjectListResponse search(String bucketId, String prefix, String query) {
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);
        String normalizedPrefix = KeyUtils.normalizePrefix(prefix);
        String regex = KeyUtils.wildcardToRegex(query == null ? "*" : query);
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

        List<S3Object> matches = new ArrayList<>();
        String token = null;
        do {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(config.bucketName())
                    .prefix(normalizedPrefix)
                    .maxKeys(500);
            if (token != null) {
                builder.continuationToken(token);
            }
            ListObjectsV2Response response = client.listObjectsV2(builder.build());
            response.contents().stream()
                    .filter(obj -> {
                        String candidate = obj.key();
                        if (!normalizedPrefix.isBlank() && candidate.startsWith(normalizedPrefix)) {
                            candidate = candidate.substring(normalizedPrefix.length());
                        }
                        String name = KeyUtils.extractName(candidate);
                        return pattern.matcher(candidate).matches() || pattern.matcher(name).matches();
                    })
                    .forEach(matches::add);
            token = response.nextContinuationToken();
        } while (token != null);

        List<ObjectItem> items = matches.stream()
                .filter(o -> !o.key().endsWith("/"))
                .map(o -> toObjectItem(client, config.bucketName(), o))
                .toList();
        return new ObjectListResponse(normalizedPrefix, Collections.emptyList(), items, null);
    }

    public ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> download(
            String bucketId, String key) {
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(config.bucketName())
                .key(key)
                .build();
        try {
            return client.getObject(request);
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Object not found");
        }
    }

    public ObjectItem copy(String bucketId, CopyMoveRequest request) {
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);
        if (!request.overwrite() && exists(client, config.bucketName(), request.targetKey())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Target already exists");
        }

        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .copySource(config.bucketName() + "/" + request.sourceKey())
                .destinationBucket(config.bucketName())
                .destinationKey(request.targetKey())
                .build();
        client.copyObject(copyRequest);
        return head(client, config.bucketName(), request.targetKey());
    }

    public ObjectItem move(String bucketId, CopyMoveRequest request) {
        ObjectItem copied = copy(bucketId, request);
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(config.bucketName())
                .key(request.sourceKey())
                .build());
        return copied;
    }

    public List<String> deleteObjects(String bucketId, DeleteObjectsRequest body) {
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);
        List<String> allKeys = new ArrayList<>(body.keys());
        for (String prefix : body.prefixes()) {
            String normalized = KeyUtils.normalizePrefix(prefix);
            allKeys.addAll(listKeys(client, config.bucketName(), normalized));
        }

        List<List<String>> chunks = chunk(allKeys, 900);
        List<String> deleted = new ArrayList<>();
        for (List<String> chunk : chunks) {
            Delete delete = Delete.builder()
                    .objects(chunk.stream().map(key -> ObjectIdentifier.builder().key(key).build()).toList())
                    .build();
            DeleteObjectsResponse response = client.deleteObjects(software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
                    .builder()
                    .bucket(config.bucketName())
                    .delete(delete)
                    .build());
            response.deleted().forEach(d -> deleted.add(d.key()));
        }
        return deleted;
    }

    public long deleteFolder(String bucketId, DeleteFolderRequest request) {
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);
        String normalizedPrefix = KeyUtils.normalizePrefix(request.prefix());
        List<String> keys = new ArrayList<>();
        String token = null;
        do {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(config.bucketName())
                    .prefix(normalizedPrefix)
                    .maxKeys(500);
            if (token != null) builder.continuationToken(token);
            ListObjectsV2Response response = client.listObjectsV2(builder.build());
            response.contents().forEach(obj -> keys.add(obj.key()));
            token = response.nextContinuationToken();
        } while (token != null);

        deleteObjects(bucketId, new DeleteObjectsRequest(keys, Collections.emptyList()));
        return keys.size();
    }

    public List<BulkOperationResult> bulkCopy(String bucketId, BulkCopyMoveRequest request) {
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);
        List<BulkOperationResult> results = new ArrayList<>();
        for (var item : request.items()) {
            if (!request.overwrite() && exists(client, config.bucketName(), item.targetKey())) {
                results.add(new BulkOperationResult(item.sourceKey(), item.targetKey(), false, "Target exists"));
                continue;
            }
            try {
                client.copyObject(CopyObjectRequest.builder()
                        .copySource(config.bucketName() + "/" + item.sourceKey())
                        .destinationBucket(config.bucketName())
                        .destinationKey(item.targetKey())
                        .build());
                results.add(new BulkOperationResult(item.sourceKey(), item.targetKey(), true, "copied"));
            } catch (S3Exception ex) {
                results.add(new BulkOperationResult(
                        item.sourceKey(), item.targetKey(), false, "Copy failed: " + ex.awsErrorDetails().errorMessage()));
            }
        }
        return results;
    }

    public List<BulkOperationResult> bulkMove(String bucketId, BulkCopyMoveRequest request) {
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);
        List<BulkOperationResult> results = new ArrayList<>();
        for (var item : request.items()) {
            if (!request.overwrite() && exists(client, config.bucketName(), item.targetKey())) {
                results.add(new BulkOperationResult(item.sourceKey(), item.targetKey(), false, "Target exists"));
                continue;
            }
            try {
                client.copyObject(CopyObjectRequest.builder()
                        .copySource(config.bucketName() + "/" + item.sourceKey())
                        .destinationBucket(config.bucketName())
                        .destinationKey(item.targetKey())
                        .build());
                client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(config.bucketName())
                        .key(item.sourceKey())
                        .build());
                results.add(new BulkOperationResult(item.sourceKey(), item.targetKey(), true, "moved"));
            } catch (S3Exception ex) {
                results.add(new BulkOperationResult(
                        item.sourceKey(), item.targetKey(), false, "Move failed: " + ex.awsErrorDetails().errorMessage()));
            }
        }
        return results;
    }

    public FolderOperationResult copyFolder(String bucketId, FolderCopyRequest request) {
        return handleFolderOperation(bucketId, request, false);
    }

    public FolderOperationResult moveFolder(String bucketId, FolderCopyRequest request) {
        return handleFolderOperation(bucketId, request, true);
    }

    public FolderSizeResponse folderSize(String bucketId, String prefix) {
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);
        String normalizedPrefix = KeyUtils.normalizePrefix(prefix);
        long total = 0;
        long count = 0;
        String token = null;
        do {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(config.bucketName())
                    .prefix(normalizedPrefix)
                    .maxKeys(500);
            if (token != null) builder.continuationToken(token);
            ListObjectsV2Response response = client.listObjectsV2(builder.build());
            for (S3Object obj : response.contents()) {
                if (!obj.key().endsWith("/")) {
                    total += obj.size();
                    count++;
                }
            }
            token = response.nextContinuationToken();
        } while (token != null);
        return new FolderSizeResponse(normalizedPrefix, total, count);
    }

    private ObjectItem toObjectItem(S3Client client, String bucketName, S3Object object) {
        HeadObjectResponse head = client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(object.key())
                .build());
        return new ObjectItem(
                object.key(),
                KeyUtils.extractName(object.key()),
                object.size(),
                object.lastModified().atZone(ZoneOffset.UTC).toInstant(),
                Optional.ofNullable(head.contentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE));
    }

    private ObjectItem head(S3Client client, String bucketName, String key) {
        HeadObjectResponse head = client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
        return new ObjectItem(
                key,
                KeyUtils.extractName(key),
                head.contentLength(),
                head.lastModified().atZone(ZoneOffset.UTC).toInstant(),
                Optional.ofNullable(head.contentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE));
    }

    private boolean exists(S3Client client, String bucket, String key) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) return false;
            throw ex;
        }
    }

    private List<List<String>> chunk(List<String> keys, int size) {
        if (keys == null || keys.isEmpty()) return List.of();
        List<List<String>> parts = new ArrayList<>();
        for (int i = 0; i < keys.size(); i += size) {
            parts.add(keys.subList(i, Math.min(i + size, keys.size())));
        }
        return parts;
    }

    private List<String> listKeys(S3Client client, String bucket, String prefix) {
        List<String> keys = new ArrayList<>();
        String token = null;
        do {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .maxKeys(500);
            if (token != null) builder.continuationToken(token);
            ListObjectsV2Response response = client.listObjectsV2(builder.build());
            response.contents().forEach(obj -> keys.add(obj.key()));
            token = response.nextContinuationToken();
        } while (token != null);
        return keys;
    }

    private FolderOperationResult handleFolderOperation(String bucketId, FolderCopyRequest request, boolean deleteSource) {
        BucketConfig config = bucketRegistry.require(bucketId);
        S3Client client = s3ClientFactory.clientFor(config);
        String sourcePrefix = KeyUtils.normalizePrefix(request.sourcePrefix());
        String targetPrefix = KeyUtils.normalizePrefix(request.targetPrefix());
        List<String> keys = listKeys(client, config.bucketName(), sourcePrefix);
        int copied = 0;
        int skipped = 0;
        List<BulkOperationResult> errors = new ArrayList<>();

        for (String key : keys) {
            if (key.endsWith("/")) {
                continue;
            }
            String relative = key.substring(sourcePrefix.length());
            String targetKey = targetPrefix + relative;
            if (!request.overwrite() && exists(client, config.bucketName(), targetKey)) {
                skipped++;
                errors.add(new BulkOperationResult(key, targetKey, false, "Target exists and overwrite=false"));
                continue;
            }
            try {
                client.copyObject(CopyObjectRequest.builder()
                        .copySource(config.bucketName() + "/" + key)
                        .destinationBucket(config.bucketName())
                        .destinationKey(targetKey)
                        .build());
                if (deleteSource) {
                    client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(config.bucketName())
                            .key(key)
                            .build());
                }
                copied++;
            } catch (S3Exception ex) {
                errors.add(new BulkOperationResult(
                        key,
                        targetKey,
                        false,
                        "Failed: " + (ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage())));
            }
        }

        return new FolderOperationResult(sourcePrefix, targetPrefix, keys.size(), copied, skipped, errors);
    }

}
