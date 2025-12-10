package com.example.s3webapp.model;

import java.time.Instant;

public record ObjectItem(String key, String name, long sizeBytes, Instant lastModified, String contentType) {}
