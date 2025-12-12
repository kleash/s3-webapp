package com.example.s3webapp.controller;

import com.example.s3webapp.model.BulkCopyMoveRequest;
import com.example.s3webapp.model.CopyMoveRequest;
import com.example.s3webapp.model.DeleteFolderRequest;
import com.example.s3webapp.model.DeleteObjectsRequest;
import com.example.s3webapp.model.FolderCopyRequest;
import com.example.s3webapp.model.FolderOperationResult;
import com.example.s3webapp.model.ObjectItem;
import com.example.s3webapp.model.ObjectListResponse;
import com.example.s3webapp.s3.StorageService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@RestController
@RequestMapping("/api/buckets/{bucketId}")
public class ObjectController {

    private final StorageService storageService;

    public ObjectController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/objects")
    public ObjectListResponse list(
            @PathVariable("bucketId") String bucketId,
            @RequestParam(value = "prefix", required = false) String prefix,
            @RequestParam(value = "pageToken", required = false) String pageToken) {
        return storageService.listObjects(bucketId, prefix, pageToken);
    }

    @GetMapping("/search")
    public ObjectListResponse search(
            @PathVariable("bucketId") String bucketId,
            @RequestParam("query") String query,
            @RequestParam(value = "prefix", required = false) String prefix) {
        return storageService.search(bucketId, prefix, query);
    }

    @GetMapping("/objects/download")
    public ResponseEntity<byte[]> download(
            @PathVariable("bucketId") String bucketId, @RequestParam("key") String key) throws IOException {
        ResponseInputStream<GetObjectResponse> stream = storageService.download(bucketId, key);
        byte[] body = stream.readAllBytes();
        String filename = URLEncoder.encode(extractName(key), StandardCharsets.UTF_8);
        String contentType = java.util.Optional.ofNullable(stream.response().contentType())
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }

    @PostMapping("/objects/copy")
    public ObjectItem copy(
            @PathVariable("bucketId") String bucketId, @Valid @RequestBody CopyMoveRequest request) {
        return storageService.copy(bucketId, request);
    }

    @PostMapping("/objects/move")
    public ObjectItem move(
            @PathVariable("bucketId") String bucketId, @Valid @RequestBody CopyMoveRequest request) {
        return storageService.move(bucketId, request);
    }

    @DeleteMapping("/objects")
    public ResponseEntity<?> delete(
            @PathVariable("bucketId") String bucketId, @Valid @RequestBody DeleteObjectsRequest request) {
        if (request.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "No keys or prefixes provided"));
        }
        return ResponseEntity.ok(storageService.deleteObjects(bucketId, request));
    }

    @DeleteMapping("/folders")
    public ResponseEntity<?> deleteFolder(
            @PathVariable("bucketId") String bucketId, @Valid @RequestBody DeleteFolderRequest request) {
        long count = storageService.deleteFolder(bucketId, request);
        return ResponseEntity.ok(java.util.Map.of("deletedCount", count));
    }

    @PostMapping("/objects/bulk-copy")
    public ResponseEntity<?> bulkCopy(
            @PathVariable("bucketId") String bucketId, @Valid @RequestBody BulkCopyMoveRequest request) {
        return ResponseEntity.ok(storageService.bulkCopy(bucketId, request));
    }

    @PostMapping("/objects/bulk-move")
    public ResponseEntity<?> bulkMove(
            @PathVariable("bucketId") String bucketId, @Valid @RequestBody BulkCopyMoveRequest request) {
        return ResponseEntity.ok(storageService.bulkMove(bucketId, request));
    }

    @PostMapping("/folders/copy")
    public FolderOperationResult copyFolder(
            @PathVariable("bucketId") String bucketId, @Valid @RequestBody FolderCopyRequest request) {
        return storageService.copyFolder(bucketId, request);
    }

    @PostMapping("/folders/move")
    public FolderOperationResult moveFolder(
            @PathVariable("bucketId") String bucketId, @Valid @RequestBody FolderCopyRequest request) {
        return storageService.moveFolder(bucketId, request);
    }

    private String extractName(String key) {
        int idx = key.lastIndexOf('/') + 1;
        return key.substring(idx);
    }
}
