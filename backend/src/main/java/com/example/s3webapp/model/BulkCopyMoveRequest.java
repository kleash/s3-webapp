package com.example.s3webapp.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkCopyMoveRequest(@NotEmpty @Valid List<BulkCopyMoveItem> items, boolean overwrite) {}
