package com.example.s3webapp.foldersize;

import com.example.s3webapp.config.FolderSizeProperties;
import com.example.s3webapp.s3.FolderSizeCalculator;
import com.example.s3webapp.s3.FolderSizeCancelledException;
import com.example.s3webapp.s3.FolderSizeComputation;
import com.example.s3webapp.s3.FolderSizeLimits;
import com.example.s3webapp.util.KeyUtils;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FolderSizeJobService {

    private static final Logger log = LoggerFactory.getLogger(FolderSizeJobService.class);

    private final FolderSizeCalculator calculator;
    private final FolderSizeProperties properties;
    private final ExecutorService executor;
    private final Map<String, FolderSizeJob> jobs = new ConcurrentHashMap<>();

    public FolderSizeJobService(FolderSizeCalculator calculator, FolderSizeProperties properties) {
        this.calculator = calculator;
        this.properties = properties;
        this.executor = Executors.newFixedThreadPool(
                properties.maxParallelJobs(), new FolderSizeThreadFactory("folder-size-worker"));
    }

    public FolderSizeJobLaunchResponse start(String bucketId, String prefix) {
        String normalizedPrefix = KeyUtils.normalizePrefix(prefix);
        FolderSizeJob job = new FolderSizeJob(bucketId, normalizedPrefix);
        jobs.put(job.id(), job);
        job.setFuture(executor.submit(() -> execute(job)));
        return new FolderSizeJobLaunchResponse(job.view(), websocketPath(job.id()));
    }

    public FolderSizeJobView get(String bucketId, String jobId) {
        FolderSizeJob job = require(jobId);
        if (!Objects.equals(job.bucketId(), bucketId)) {
            throw new IllegalArgumentException("Job does not belong to bucket " + bucketId);
        }
        return job.view();
    }

    public FolderSizeJobView cancel(String bucketId, String jobId) {
        FolderSizeJob job = require(jobId);
        if (!Objects.equals(job.bucketId(), bucketId)) {
            throw new IllegalArgumentException("Job does not belong to bucket " + bucketId);
        }
        return cancel(job);
    }

    public FolderSizeJobView cancel(String jobId) {
        FolderSizeJob job = require(jobId);
        return cancel(job);
    }

    public FolderSizeJobView attachListener(String jobId, String listenerId, Consumer<FolderSizeEvent> listener) {
        FolderSizeJob job = require(jobId);
        job.addListener(listenerId, listener);
        listener.accept(job.event("SNAPSHOT"));
        return job.view();
    }

    public void detachListener(String jobId, String listenerId) {
        FolderSizeJob job = jobs.get(jobId);
        if (job == null) return;
        job.removeListener(listenerId);
        if (properties.cancelOnDisconnect() && !job.isTerminal() && !job.hasListeners()) {
            log.info("Canceling folder size job {} because all listeners detached", jobId);
            job.requestCancel();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void execute(FolderSizeJob job) {
        log.info("Starting folder size job {} for {}/{}", job.id(), job.bucketId(), job.prefix());
        job.markRunning();
        broadcast(job, "STARTED");
        FolderSizeLimits limits = new FolderSizeLimits(properties.maxObjects(), properties.maxRuntime());
        try {
            FolderSizeComputation result = calculator.compute(
                    job.bucketId(),
                    job.prefix(),
                    limits,
                    job::cancelRequested,
                    computation -> onProgress(job, computation),
                    properties.progressPageInterval());
            job.markCompleted(result);
            if (result.partial()) {
                job.setMessage(partialMessage(result.partialReason()));
                broadcast(job, "PARTIAL");
            } else {
                broadcast(job, "COMPLETED");
            }
        } catch (FolderSizeCancelledException ex) {
            job.markCanceled();
            broadcast(job, "CANCELED");
        } catch (Exception ex) {
            job.markFailed(ex.getMessage());
            broadcast(job, "FAILED");
        }
    }

    private void onProgress(FolderSizeJob job, FolderSizeComputation computation) {
        if (computation.finished()) {
            return;
        }
        job.applyProgress(computation);
        broadcast(job, "PROGRESS");
    }

    private void broadcast(FolderSizeJob job, String eventType) {
        FolderSizeEvent event = job.event(eventType);
        for (var listener : job.listeners()) {
            try {
                listener.accept(event);
            } catch (Exception ex) {
                log.warn("Failed to deliver folder size event for job {}: {}", job.id(), ex.getMessage());
            }
        }
    }

    private FolderSizeJob require(String jobId) {
        FolderSizeJob job = jobs.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Unknown job: " + jobId);
        }
        return job;
    }

    private FolderSizeJobView cancel(FolderSizeJob job) {
        if (job.isTerminal()) {
            return job.view();
        }
        job.requestCancel();
        job.markCanceled();
        broadcast(job, "CANCELED");
        return job.view();
    }

    @Scheduled(fixedDelayString = "PT1M")
    void cleanupCompleted() {
        Instant cutoff = Instant.now().minus(properties.retention());
        jobs.entrySet().removeIf(entry -> {
            FolderSizeJob job = entry.getValue();
            Instant finished = job.view().finishedAt();
            return job.isTerminal() && finished != null && finished.isBefore(cutoff);
        });
    }

    public String websocketPath(String jobId) {
        return "/api/ws/folder-size/" + jobId;
    }

    private String partialMessage(String reason) {
        if (reason == null) return "Stopped early";
        return switch (reason) {
            case "max-objects" -> "Stopped after hitting max-objects cap";
            case "max-runtime" -> "Stopped after hitting max-runtime cap";
            default -> "Stopped early: " + reason;
        };
    }

    private static class FolderSizeThreadFactory implements ThreadFactory {
        private final String prefix;

        FolderSizeThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(prefix + "-" + UUID.randomUUID());
            thread.setDaemon(true);
            return thread;
        }
    }
}
