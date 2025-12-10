package com.example.s3webapp.model;

import jakarta.validation.constraints.NotBlank;

public record BulkCopyMoveItem(@NotBlank String sourceKey, @NotBlank String targetKey) {}
