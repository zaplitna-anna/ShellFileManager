package com.shell.operation;

import com.shell.model.OperationProgress;
import com.shell.observer.OperationProgressPublisher;

import java.nio.file.Path;
import java.util.Objects;

public class OperationContext {
    private static final long PROGRESS_UPDATE_INTERVAL =
            50_000_000L;

    private final CancellationToken cancellationToken;
    private final OperationProgressPublisher progressPublisher;

    private long processedBytes;
    private long totalBytes;

    private long processedItems;
    private long totalItems;

    private Path currentPath;
    private long lastProgressUpdate;
    private boolean progressPublished;

    public OperationContext(
            CancellationToken cancellationToken,
            OperationProgressPublisher progressPublisher
    ) {
        this.cancellationToken = Objects.requireNonNull(
                cancellationToken,
                "Механізм скасування не може бути null."
        );

        this.progressPublisher = Objects.requireNonNull(
                progressPublisher,
                "Видавець прогресу не може бути null."
        );
    }

    public void setTotalWork(
            long totalBytes,
            long totalItems
    ) {
        if (totalBytes < 0 || totalItems < 0) {
            throw new IllegalArgumentException(
                    "Загальний обсяг роботи не може "
                            + "бути від'ємним."
            );
        }

        this.totalBytes = totalBytes;
        this.totalItems = totalItems;

        publishFinalProgress();
    }

    public void setCurrentPath(Path currentPath) {
        this.currentPath = Objects.requireNonNull(
                currentPath,
                "Поточний шлях не може бути null."
        );

        publishProgress();
    }

    public void addProcessedBytes(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException(
                    "Кількість оброблених байтів "
                            + "не може бути від'ємною."
            );
        }

        processedBytes += bytes;

        publishProgress();
    }

    public void addProcessedItems(long items) {
        if (items < 0) {
            throw new IllegalArgumentException(
                    "Кількість оброблених об'єктів "
                            + "не може бути від'ємною."
            );
        }

        processedItems += items;

        publishProgress();
    }

    public void checkCancellation() {
        cancellationToken.checkCancellation();
    }

    public OperationProgress getProgress() {
        return new OperationProgress(
                processedBytes,
                totalBytes,
                processedItems,
                totalItems,
                currentPath
        );
    }


    private void publishProgress() {
        long currentTime = System.nanoTime();

        if (!progressPublished
                || currentTime - lastProgressUpdate
                >= PROGRESS_UPDATE_INTERVAL) {

            progressPublisher.notifyObservers(getProgress());

            lastProgressUpdate = currentTime;
            progressPublished = true;
        }
    }

    public void publishFinalProgress() {
        progressPublisher.notifyObservers(getProgress());

        lastProgressUpdate = System.nanoTime();
        progressPublished = true;
    }
}
