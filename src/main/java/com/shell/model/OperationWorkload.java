package com.shell.model;

public record OperationWorkload(
        long totalBytes,
        long totalItems
) {
    public OperationWorkload {
        if (totalBytes < 0 || totalItems < 0) {
            throw new IllegalArgumentException(
                    "Обсяг файлової операції не може "
                            + "бути від'ємним."
            );
        }
    }
}
