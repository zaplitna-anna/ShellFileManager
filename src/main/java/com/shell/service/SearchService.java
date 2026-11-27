package com.shell.service;

import com.shell.facade.FileSystemFacade;
import com.shell.model.FileSystemItem;
import com.shell.model.SearchMode;
import com.shell.operation.CancellationToken;
import com.shell.strategy.search.ExactSearchStrategy;
import com.shell.strategy.search.PartialSearchStrategy;
import com.shell.strategy.search.SearchStrategy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class SearchService {

    private final FileSystemFacade fileSystemFacade;

    public SearchService(
            FileSystemFacade fileSystemFacade
    ) {
        this.fileSystemFacade = Objects.requireNonNull(
                fileSystemFacade,
                "Фасад файлової системи не може бути null."
        );
    }

    public List<FileSystemItem> search(
            Path directory,
            String query,
            SearchMode mode,
            CancellationToken cancellationToken
    ) throws IOException {

        Objects.requireNonNull(
                mode,
                "Режим пошуку не може бути null."
        );

        SearchStrategy strategy = switch (mode) {
            case EXACT -> new ExactSearchStrategy();
            case PARTIAL -> new PartialSearchStrategy();
        };

        return fileSystemFacade.search(
                directory,
                query,
                strategy,
                cancellationToken
        );
    }
}
