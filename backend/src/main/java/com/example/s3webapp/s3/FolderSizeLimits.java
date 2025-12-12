package com.example.s3webapp.s3;

import java.time.Duration;

public record FolderSizeLimits(long maxObjects, Duration maxRuntime) {

    public FolderSizeLimits {
        maxObjects = Math.max(0, maxObjects);
        maxRuntime = maxRuntime == null ? Duration.ZERO : maxRuntime;
    }

    public static FolderSizeLimits unbounded() {
        return new FolderSizeLimits(0, Duration.ZERO);
    }

    public boolean hasObjectCap() {
        return maxObjects > 0;
    }

    public boolean hasRuntimeCap() {
        return maxRuntime != null && !maxRuntime.isZero() && !maxRuntime.isNegative();
    }
}
