package com.shell.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final Path databasePath;
    private final String connectionUrl;

    public DatabaseManager() {

        String localAppData =
                System.getenv("LOCALAPPDATA");

        Path applicationDirectory;

        if (localAppData != null
                && !localAppData.isBlank()) {

            applicationDirectory =
                    Path.of(localAppData, "Shell");

        } else {

            applicationDirectory =
                    Path.of(
                            System.getProperty("user.home"),
                            ".shell"
                    );
        }

        databasePath = applicationDirectory
                .resolve("shell.db")
                .toAbsolutePath()
                .normalize();

        connectionUrl =
                "jdbc:sqlite:" + databasePath;
    }

    public Connection getConnection()
            throws SQLException {

        return DriverManager.getConnection(
                connectionUrl
        );
    }

    public void initialize()
            throws IOException, SQLException {

        Files.createDirectories(
                databasePath.getParent()
        );

        String createSearchHistoryTable = """
                CREATE TABLE IF NOT EXISTS search_history (
                    id INTEGER PRIMARY KEY,
                    query TEXT NOT NULL,
                    search_mode TEXT NOT NULL,
                    directory_path TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
                """;

        String createOperationHistoryTable = """
                CREATE TABLE IF NOT EXISTS operation_history (
                    id INTEGER PRIMARY KEY,
                    operation_type TEXT NOT NULL,
                    source_path TEXT,
                    destination_path TEXT,
                    status TEXT NOT NULL,
                    message TEXT,
                    created_at TEXT NOT NULL
                )
                """;

        try (Connection connection = getConnection();
             Statement statement =
                     connection.createStatement()) {

            statement.executeUpdate(
                    createSearchHistoryTable
            );

            statement.executeUpdate(
                    createOperationHistoryTable
            );
        }
    }

    public Path getDatabasePath() {
        return databasePath;
    }
}
