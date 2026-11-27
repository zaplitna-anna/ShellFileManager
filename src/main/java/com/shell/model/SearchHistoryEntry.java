package com.shell.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

public record SearchHistoryEntry(
        long id,
        String query,
        SearchMode searchMode,
        Path directory,
        LocalDateTime createdAt
) {
}
