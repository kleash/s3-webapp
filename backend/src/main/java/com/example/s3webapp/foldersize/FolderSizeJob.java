package com.example.s3webapp.foldersize;

import com.example.s3webapp.s3.FolderSizeComputation;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

class FolderSizeJob {
    private final String id = UUID.randomUUID().toString();
    private final String bucketId;
    private final String prefix;
    private final Instant createdAt = Instant.now();
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private final Map<String, Consumer<FolderSizeEvent>> listeners = new ConcurrentHashMap<>();

    private volatile FolderSizeStatus status = FolderSizeStatus.QUEUED;
    private volatile long objectsScanned = 0;
    private volatile long totalSizeBytes = 0;
    private volatile boolean partial = false;
    private volatile String partialReason = null;
    private volatile String message = null;
    private volatile Instant startedAt = null;
    private volatile Instant finishedAt = null;
    private volatile Future<?> future;

    FolderSizeJob(String bucketId, String prefix) {
        this.bucketId = bucketId;
        this.prefix = prefix;
    }

    String id() {
        return id;
    }

    String bucketId() {
        return bucketId;
    }

    String prefix() {
        return prefix;
    }

    Instant createdAt() {
        return createdAt;
    }

    boolean cancelRequested() {
        return cancelRequested.get();
    }

    void requestCancel() {
        cancelRequested.set(true);
        if (future != null) {
            future.cancel(true);
        }
    }

    void setFuture(Future<?> future) {
        this.future = future;
    }

    void markRunning() {
        this.status = FolderSizeStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    void markCompleted(FolderSizeComputation computation) {
        applyProgress(computation);
        this.status = FolderSizeStatus.COMPLETED;
        this.finishedAt = Instant.now();
    }

    void markFailed(String message) {
        this.status = FolderSizeStatus.FAILED;
        this.message = message;
        this.finishedAt = Instant.now();
    }

    void markCanceled() {
        this.status = FolderSizeStatus.CANCELED;
        this.message = "Canceled";
        this.finishedAt = Instant.now();
    }

    void applyProgress(FolderSizeComputation computation) {
        this.objectsScanned = computation.objectsScanned();
        this.totalSizeBytes = computation.totalSizeBytes();
        this.partial = computation.partial();
        this.partialReason = computation.partialReason();
    }

    boolean isTerminal() {
        return status == FolderSizeStatus.COMPLETED
                || status == FolderSizeStatus.FAILED
                || status == FolderSizeStatus.CANCELED;
    }

    FolderSizeJobView view() {
        return new FolderSizeJobView(
                id,
                bucketId,
                prefix,
                status,
                objectsScanned,
                totalSizeBytes,
                partial,
                partialReason,
                message,
                startedAt,
                finishedAt);
    }

    void setMessage(String message) {
        this.message = message;
    }

    FolderSizeEvent event(String eventType) {
        return new FolderSizeEvent(eventType, view());
    }

    void addListener(String subscriberId, Consumer<FolderSizeEvent> listener) {
        listeners.put(subscriberId, listener);
    }

    void removeListener(String subscriberId) {
        listeners.remove(subscriberId);
    }

    CopyOnWriteArrayList<Consumer<FolderSizeEvent>> listeners() {
        return new CopyOnWriteArrayList<>(listeners.values());
    }

    boolean hasListeners() {
        return !listeners.isEmpty();
    }
}
