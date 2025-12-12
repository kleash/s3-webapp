package com.example.s3webapp.s3;

public class FolderSizeCancelledException extends RuntimeException {
    public FolderSizeCancelledException(String message) {
        super(message);
    }
}
