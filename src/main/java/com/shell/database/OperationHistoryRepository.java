package com.shell.database;

import com.shell.model.OperationHistoryEntry;
import com.shell.model.OperationStatus;
import com.shell.observer.FileSystemEventType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OperationHistoryRepository {

    private final DatabaseManager databaseManager;

    public OperationHistoryRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(
                databaseManager, "Менеджер бази даних не може бути null."
        );
    }

    public void save(FileSystemEventType type, String sourcePath,
                     String destinationPath, OperationStatus status,
                     String message) throws SQLException {
        String sql = """
                INSERT INTO operation_history (
                    operation_type, source_path, destination_path,
                    status, message, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type.name());
            statement.setString(2, sourcePath);
            statement.setString(3, destinationPath);
            statement.setString(4, status.name());
            statement.setString(5, message);
            statement.setString(6, LocalDateTime.now().toString());
            statement.executeUpdate();
        }
    }

    public List<OperationHistoryEntry> findAll() throws SQLException {
        String sql = """
                SELECT id, operation_type, source_path, destination_path,
                       status, message, created_at
                FROM operation_history
                ORDER BY id DESC
                """;
        List<OperationHistoryEntry> history = new ArrayList<>();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                history.add(new OperationHistoryEntry(
                        resultSet.getLong("id"),
                        FileSystemEventType.valueOf(resultSet.getString("operation_type")),
                        resultSet.getString("source_path"),
                        resultSet.getString("destination_path"),
                        OperationStatus.valueOf(resultSet.getString("status")),
                        resultSet.getString("message"),
                        LocalDateTime.parse(resultSet.getString("created_at"))
                ));
            }
        }
        return history;
    }
}
