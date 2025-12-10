package com.example.s3webapp.s3;

import com.example.s3webapp.config.S3Properties;
import com.example.s3webapp.config.S3Properties.BucketConfig;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BucketRegistry {

    private final Map<String, BucketConfig> bucketsById;

    public BucketRegistry(S3Properties properties) {
        this.bucketsById = properties.buckets().stream()
                .collect(Collectors.toMap(BucketConfig::id, b -> b));
    }

    @PostConstruct
    void validate() {
        if (bucketsById.isEmpty()) {
            throw new IllegalStateException("No S3 buckets configured. Add entries under s3.buckets.");
        }
    }

    public List<BucketConfig> list() {
        return List.copyOf(bucketsById.values());
    }

    public BucketConfig require(String id) {
        return Optional.ofNullable(bucketsById.get(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown bucket id: " + id));
    }
}
