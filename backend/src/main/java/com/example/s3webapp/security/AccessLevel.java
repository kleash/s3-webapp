package com.example.s3webapp.security;

public enum AccessLevel {
    READ_ONLY,
    READ_WRITE;

    public boolean canWrite() {
        return this == READ_WRITE;
    }
}
