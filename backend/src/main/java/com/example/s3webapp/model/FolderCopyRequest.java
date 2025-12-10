package com.example.s3webapp.model;

import jakarta.validation.constraints.NotBlank;

public record FolderCopyRequest(@NotBlank String sourcePrefix, @NotBlank String targetPrefix, boolean overwrite) {}
