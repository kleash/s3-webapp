package com.example.s3webapp.model;

import jakarta.validation.constraints.NotBlank;

public record CopyMoveRequest(@NotBlank String sourceKey, @NotBlank String targetKey, boolean overwrite) {}
