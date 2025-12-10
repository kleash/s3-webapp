package com.example.s3webapp.model;

import java.util.List;

public record ObjectListResponse(
        String currentPrefix,
        List<FolderItem> folders,
        List<ObjectItem> objects,
        String nextPageToken) {}
