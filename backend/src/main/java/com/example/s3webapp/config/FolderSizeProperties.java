package com.example.s3webapp.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.folder-size")
public record FolderSizeProperties(
        int maxParallelJobs,
        int progressPageInterval,
        long maxObjects,
        Duration maxRuntime,
        Duration retention,
        boolean cancelOnDisconnect) {

    public FolderSizeProperties {
        maxParallelJobs = maxParallelJobs > 0 ? maxParallelJobs : 2;
        progressPageInterval = progressPageInterval > 0 ? progressPageInterval : 1;
        maxObjects = Math.max(0, maxObjects);
        maxRuntime = maxRuntime == null ? Duration.ZERO : maxRuntime;
        retention = retention == null || retention.isZero() || retention.isNegative()
                ? Duration.ofMinutes(10)
                : retention;
    }

    public boolean hasObjectCap() {
        return maxObjects > 0;
    }

    public boolean hasRuntimeCap() {
        return maxRuntime != null && !maxRuntime.isZero() && !maxRuntime.isNegative();
    }
}
