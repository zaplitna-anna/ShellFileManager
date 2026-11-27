package com.shell.model;

import java.nio.file.Path;

public record OperationProgress(
        long processedBytes,
        long totalBytes,
        long processedItems,
        long totalItems,
        Path currentPath
) {

    public double getProgress() {
        if (totalBytes > 0) {
            return Math.max(
                    0.0,
                    Math.min(
                            1.0,
                            (double) processedBytes / totalBytes
                    )
            );
        }

        if (totalItems > 0) {
            return Math.max(
                    0.0,
                    Math.min(
                            1.0,
                            (double) processedItems / totalItems
                    )
            );
        }

        return -1.0;
    }
}