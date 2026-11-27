package com.shell.service;

import com.shell.database.SearchHistoryRepository;
import com.shell.facade.FileSystemFacade;
import com.shell.model.FileSystemItem;
import com.shell.model.SearchMode;
import com.shell.operation.CancellationToken;
import com.shell.strategy.search.ExactSearchStrategy;
import com.shell.strategy.search.PartialSearchStrategy;
import com.shell.strategy.search.SearchStrategy;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.shell.model.SearchHistoryEntry;

public class SearchService {

    private static final Logger LOGGER =
            Logger.getLogger(
                    SearchService.class.getName()
            );

    private final FileSystemFacade fileSystemFacade;

    private final SearchHistoryRepository
            searchHistoryRepository;

    public SearchService(
            FileSystemFacade fileSystemFacade,
            SearchHistoryRepository searchHistoryRepository
    ) {
        this.fileSystemFacade = Objects.requireNonNull(
                fileSystemFacade,
                "Фасад файлової системи не може бути null."
        );

        this.searchHistoryRepository =
                Objects.requireNonNull(
                        searchHistoryRepository,
                        "Репозиторій історії пошуку "
                                + "не може бути null."
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

        List<FileSystemItem> results =
                fileSystemFacade.search(
                        directory,
                        query,
                        strategy,
                        cancellationToken
                );

        cancellationToken.checkCancellation();

        try {
            searchHistoryRepository.save(
                    query,
                    mode,
                    directory
            );

        } catch (SQLException exception) {

            LOGGER.log(
                    Level.WARNING,
                    "Не вдалося зберегти "
                            + "історію пошуку.",
                    exception
            );
        }

        return results;
    }

    public List<SearchHistoryEntry> getSearchHistory()
            throws SQLException {

        return searchHistoryRepository.findAll();
    }
}
