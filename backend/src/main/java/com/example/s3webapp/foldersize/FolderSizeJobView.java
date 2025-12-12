package com.example.s3webapp.foldersize;

import java.time.Instant;

public record FolderSizeJobView(
        String id,
        String bucketId,
        String prefix,
        FolderSizeStatus status,
        long objectsScanned,
        long totalSizeBytes,
        boolean partial,
        String partialReason,
        String message,
        Instant startedAt,
        Instant finishedAt) {}
