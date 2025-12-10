package com.example.s3webapp.model;

import java.util.Collections;
import java.util.List;

public record DeleteObjectsRequest(List<String> keys, List<String> prefixes) {
    public DeleteObjectsRequest {
        keys = keys == null ? Collections.emptyList() : keys;
        prefixes = prefixes == null ? Collections.emptyList() : prefixes;
    }

    public boolean isEmpty() {
        return keys.isEmpty() && prefixes.isEmpty();
    }
}
