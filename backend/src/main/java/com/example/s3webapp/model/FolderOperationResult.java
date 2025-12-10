package com.example.s3webapp.model;

import java.util.List;

public record FolderOperationResult(
        String sourcePrefix,
        String targetPrefix,
        int totalObjects,
        int copied,
        int skipped,
        List<BulkOperationResult> errors) {}
