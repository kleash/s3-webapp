package com.example.s3webapp.model;

public record FolderSizeResponse(String prefix, long totalSizeBytes, long objectCount) {}
