package com.example.s3webapp.model;

import jakarta.validation.constraints.NotBlank;

public record DeleteFolderRequest(@NotBlank String prefix) {}
