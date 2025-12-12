package com.example.s3webapp.controller;

import com.example.s3webapp.foldersize.FolderSizeJobLaunchResponse;
import com.example.s3webapp.foldersize.FolderSizeJobService;
import com.example.s3webapp.foldersize.FolderSizeJobView;
import com.example.s3webapp.model.FolderSizeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/buckets/{bucketId}/folders/size")
public class FolderSizeController {

    private final FolderSizeJobService jobService;

    public FolderSizeController(FolderSizeJobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public FolderSizeJobLaunchResponse start(
            @PathVariable("bucketId") String bucketId, @Valid @RequestBody FolderSizeRequest request) {
        return jobService.start(bucketId, request.prefix());
    }

    @GetMapping("/{jobId}")
    public FolderSizeJobView status(
            @PathVariable("bucketId") String bucketId, @PathVariable("jobId") String jobId) {
        return jobService.get(bucketId, jobId);
    }

    @DeleteMapping("/{jobId}")
    public FolderSizeJobView cancel(
            @PathVariable("bucketId") String bucketId, @PathVariable("jobId") String jobId) {
        return jobService.cancel(bucketId, jobId);
    }
}
