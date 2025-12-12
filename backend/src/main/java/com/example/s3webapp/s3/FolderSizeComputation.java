package com.example.s3webapp.s3;

public record FolderSizeComputation(
        String prefix, long objectsScanned, long totalSizeBytes, boolean partial, String partialReason, boolean finished) {}
