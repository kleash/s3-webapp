package com.example.s3webapp.model;

import jakarta.validation.constraints.NotNull;

public record FolderSizeRequest(@NotNull String prefix) {}
