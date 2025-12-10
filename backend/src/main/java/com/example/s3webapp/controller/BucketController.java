package com.example.s3webapp.controller;

import com.example.s3webapp.model.BucketDto;
import com.example.s3webapp.s3.StorageService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/buckets")
public class BucketController {

    private final StorageService storageService;

    public BucketController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    public List<BucketDto> listBuckets() {
        return storageService.listBuckets().stream()
                .map(cfg -> new BucketDto(cfg.id(), cfg.name(), cfg.bucketName()))
                .toList();
    }
}
