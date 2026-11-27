package com.shell.database;

import com.shell.model.SearchHistoryEntry;
import com.shell.model.SearchMode;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SearchHistoryRepository {

    private final DatabaseManager databaseManager;

    public SearchHistoryRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager,
                "Менеджер бази даних не може бути null."
        );
    }

    public void save(
            String query,
            SearchMode searchMode,
            Path directory
    ) throws SQLException {

        String sql = """
                INSERT INTO search_history (
                    query,
                    search_mode,
                    directory_path,
                    created_at
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection =
                     databaseManager.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, query);

            statement.setString(
                    2,
                    searchMode.name()
            );

            statement.setString(
                    3,
                    directory.toAbsolutePath()
                            .normalize()
                            .toString()
            );

            statement.setString(
                    4,
                    LocalDateTime.now().toString()
            );

            statement.executeUpdate();
        }
    }

    public List<SearchHistoryEntry> findAll()
            throws SQLException {

        String sql = """
                SELECT
                    id,
                    query,
                    search_mode,
                    directory_path,
                    created_at
                FROM search_history
                ORDER BY id DESC
                """;

        List<SearchHistoryEntry> history =
                new ArrayList<>();

        try (Connection connection =
                     databaseManager.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {

                SearchHistoryEntry entry =
                        new SearchHistoryEntry(
                                resultSet.getLong("id"),
                                resultSet.getString("query"),
                                SearchMode.valueOf(
                                        resultSet.getString(
                                                "search_mode"
                                        )
                                ),
                                Path.of(
                                        resultSet.getString(
                                                "directory_path"
                                        )
                                ),
                                LocalDateTime.parse(
                                        resultSet.getString(
                                                "created_at"
                                        )
                                )
                        );

                history.add(entry);
            }
        }

        return history;
    }
}
