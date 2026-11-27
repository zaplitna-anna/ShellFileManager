package com.shell.observer;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record FileSystemEvent(
        FileSystemEventType type,
        Path sourceDirectory,
        Path destinationDirectory,
        List<Path> affectedPaths,
        Path newPath
) {
    public FileSystemEvent {
        Objects.requireNonNull(
                type,
                "Тип події не може бути null."
        );

        sourceDirectory = normalizePath(sourceDirectory);
        destinationDirectory = normalizePath(destinationDirectory);
        newPath = normalizePath(newPath);

        affectedPaths = List.copyOf(
                        Objects.requireNonNull(
                                affectedPaths,
                                "Список шляхів не може бути null."
                        )
                ).stream()
                .map(FileSystemEvent::normalizePath)
                .toList();
    }

    private static Path normalizePath(Path path) {
        return path == null
                ? null
                : path.toAbsolutePath().normalize();
    }
}