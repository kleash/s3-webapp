package com.example.s3webapp.s3;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.s3webapp.config.S3Properties;
import com.example.s3webapp.model.CopyMoveRequest;
import com.example.s3webapp.model.BulkCopyMoveItem;
import com.example.s3webapp.model.BulkCopyMoveRequest;
import com.example.s3webapp.model.DeleteObjectsRequest;
import com.example.s3webapp.model.FolderCopyRequest;
import com.example.s3webapp.model.FolderSizeResponse;
import com.example.s3webapp.model.ObjectListResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import io.findify.s3mock.S3Mock;

class StorageServiceIntegrationTest {

    private static final S3Mock S3_MOCK = new S3Mock.Builder().withPort(9095).withInMemoryBackend().build();

    private StorageService storageService;
    private S3Client client;
    private S3Properties.BucketConfig config;

    @BeforeAll
    static void startServer() {
        S3_MOCK.start();
    }

    @AfterAll
    static void stopServer() {
        S3_MOCK.stop();
    }

    @BeforeEach
    void setUp() {
        config = new S3Properties.BucketConfig(
                "test",
                "Test Bucket",
                "test-bucket",
                "http://localhost:9095",
                "access",
                "secret",
                "us-east-1",
                true);
        S3Properties properties = new S3Properties(List.of(config));
        BucketRegistry registry = new BucketRegistry(properties);
        S3ClientFactory factory = new S3ClientFactory();
        storageService = new StorageService(registry, factory);
        client = factory.clientFor(config);
        client.createBucket(CreateBucketRequest.builder().bucket(config.bucketName()).build());

        seed();
    }

    private void seed() {
        put("logs/app/2025/01/01/a.txt", "hello");
        put("logs/app/2025/01/02/b.log", "data");
        put("logs/app/2025/01/02/trade_2025_01.csv", "rows");
        put("root.txt", "root");
    }

    private void put(String key, String content) {
        client.putObject(PutObjectRequest.builder()
                        .bucket(config.bucketName())
                        .key(key)
                        .contentType("text/plain")
                        .build(),
                software.amazon.awssdk.core.sync.RequestBody.fromString(content, StandardCharsets.UTF_8));
    }

    @Test
    void listsObjectsByPrefix() {
        ObjectListResponse response = storageService.listObjects(config.id(), "logs/app/2025/", null);
        assertThat(response.folders()).extracting("name").contains("01");
    }

    @Test
    void searchesWithinPrefixWithWildcard() {
        ObjectListResponse response = storageService.search(config.id(), "logs/app/2025/01/02/", "trade_2025_*.csv");
        assertThat(response.objects()).extracting("name").contains("trade_2025_01.csv");
    }

    @Test
    void bulkCopyAndMoveWithPartialFailure() {
        storageService.copy(config.id(), new CopyMoveRequest("root.txt", "keep/root.txt", true));
        var request = new BulkCopyMoveRequest(
                List.of(
                        new BulkCopyMoveItem("root.txt", "dest/root.txt"),
                        new BulkCopyMoveItem("keep/root.txt", "dest/root.txt")),
                false);
        var copyResults = storageService.bulkCopy(config.id(), request);
        assertThat(copyResults).hasSize(2);
        assertThat(copyResults.get(0).success()).isTrue();
        assertThat(copyResults.get(1).success()).isFalse();

        var moveResults = storageService.bulkMove(config.id(), request);
        assertThat(moveResults).hasSize(2);
    }

    @Test
    void folderCopyAndMoveWithCollisionSkip() {
        FolderCopyRequest copyRequest =
                new FolderCopyRequest("logs/app/2025/01/01/", "root/app/2025/05/", false);
        var result = storageService.copyFolder(config.id(), copyRequest);
        assertThat(result.copied()).isGreaterThan(0);
        ObjectListResponse copied =
                storageService.search(config.id(), "root/app/2025/05/", "a.txt");
        assertThat(copied.objects()).extracting("key").contains("root/app/2025/05/a.txt");

        // Moving same source to same target should skip existing when overwrite is false
        FolderCopyRequest moveRequest =
                new FolderCopyRequest("logs/app/2025/01/01/", "root/app/2025/05/", false);
        var moveResult = storageService.moveFolder(config.id(), moveRequest);
        assertThat(moveResult.skipped()).isGreaterThanOrEqualTo(1);

        // Now move to a new prefix and ensure source is deleted
        FolderCopyRequest moveNewRequest =
                new FolderCopyRequest("logs/app/2025/01/01/", "root/app/2025/06/", true);
        storageService.moveFolder(config.id(), moveNewRequest);
        ObjectListResponse moved =
                storageService.search(config.id(), "root/app/2025/06/", "a.txt");
        assertThat(moved.objects()).extracting("key").contains("root/app/2025/06/a.txt");
        ObjectListResponse sourceGone =
                storageService.search(config.id(), "logs/app/2025/01/01/", "a.txt");
        assertThat(sourceGone.objects()).isEmpty();
    }

    @Test
    void copiesAndMovesObjects() {
        storageService.copy(config.id(), new CopyMoveRequest("root.txt", "copied/root.txt", true));
        storageService.move(config.id(), new CopyMoveRequest("copied/root.txt", "moved/root.txt", true));
        ObjectListResponse response = storageService.search(config.id(), "", "moved/root.txt");
        assertThat(response.objects()).extracting("key").contains("moved/root.txt");
    }

    @Test
    void deletesObjectsAndComputesSize() {
        List<String> deleted =
                storageService.deleteObjects(config.id(), new DeleteObjectsRequest(List.of("root.txt"), List.of()));
        assertThat(deleted).contains("root.txt");
        FolderSizeResponse size = storageService.folderSize(config.id(), "logs/app/");
        assertThat(size.objectCount()).isGreaterThanOrEqualTo(2);
    }
}
