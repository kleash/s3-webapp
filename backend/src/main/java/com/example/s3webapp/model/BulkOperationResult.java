package com.example.s3webapp.model;

public record BulkOperationResult(String sourceKey, String targetKey, boolean success, String message) {}
