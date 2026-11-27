package com.shell.model;

import com.shell.observer.FileSystemEventType;

import java.time.LocalDateTime;

public record OperationHistoryEntry(
        long id,
        FileSystemEventType operationType,
        String sourcePath,
        String destinationPath,
        OperationStatus status,
        String message,
        LocalDateTime createdAt
) {
}
