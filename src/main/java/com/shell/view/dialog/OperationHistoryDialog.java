package com.shell.view.dialog;

import com.shell.model.OperationHistoryEntry;
import com.shell.model.OperationStatus;
import com.shell.observer.FileSystemEventType;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class OperationHistoryDialog {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final Stage stage = new Stage();

    private final TableView<OperationHistoryEntry> historyTable =
            new TableView<>();

    private final TextArea sourceArea = new TextArea();
    private final TextArea destinationArea = new TextArea();
    private final TextArea messageArea = new TextArea();

    private final Button closeButton =
            new Button("Закрити");

    public OperationHistoryDialog(
            Window owner,
            List<OperationHistoryEntry> history
    ) {

        stage.setTitle("Журнал файлових операцій");
        stage.initModality(Modality.APPLICATION_MODAL);

        if (owner != null) {
            stage.initOwner(owner);
        }

        configureTable();
        configureDetails();

        historyTable.setItems(
                FXCollections.observableArrayList(history)
        );

        Label titleLabel =
                new Label("Історія файлових операцій");

        VBox detailsSection = new VBox(
                6,
                new Label("Деталі операції"),
                new Label("Початковий шлях:"),
                sourceArea,
                new Label("Шлях призначення:"),
                destinationArea,
                new Label("Повідомлення:"),
                messageArea
        );

        Label statusLabel = new Label(
                "Усього записів: " + history.size()
        );

        Region spacer = new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        HBox bottomRow = new HBox(
                10,
                statusLabel,
                spacer,
                closeButton
        );

        bottomRow.setAlignment(Pos.CENTER_LEFT);

        closeButton.setOnAction(
                event -> stage.close()
        );

        closeButton.setCancelButton(true);

        VBox root = new VBox(
                12,
                titleLabel,
                historyTable,
                new Separator(),
                detailsSection,
                bottomRow
        );

        root.setPadding(new Insets(15));

        VBox.setVgrow(
                historyTable,
                Priority.ALWAYS
        );

        Scene scene = new Scene(root, 850, 650);

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource(
                                "/css/styles.css"
                        ),
                        "Не знайдено файл стилів styles.css."
                ).toExternalForm()
        );

        stage.setScene(scene);
        stage.setMinWidth(650);
        stage.setMinHeight(580);

        stage.setOnShown(event ->
                historyTable.requestFocus()
        );

        if (!history.isEmpty()) {
            historyTable.getSelectionModel().selectFirst();
        }
    }

    private void configureTable() {

        TableColumn<OperationHistoryEntry, LocalDateTime>
                dateColumn = new TableColumn<>("Дата й час");

        dateColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        data.getValue().createdAt()
                )
        );

        dateColumn.setCellFactory(column ->
                new TableCell<>() {

                    @Override
                    protected void updateItem(
                            LocalDateTime date,
                            boolean empty
                    ) {
                        super.updateItem(date, empty);

                        setText(
                                empty || date == null
                                        ? null
                                        : date.format(DATE_FORMATTER)
                        );
                    }
                }
        );

        dateColumn.setPrefWidth(220);

        TableColumn<OperationHistoryEntry, String>
                typeColumn = new TableColumn<>("Операція");

        typeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        formatOperationType(
                                data.getValue().operationType()
                        )
                )
        );

        typeColumn.setPrefWidth(220);

        TableColumn<OperationHistoryEntry, String>
                statusColumn = new TableColumn<>("Результат");

        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        formatStatus(
                                data.getValue().status()
                        )
                )
        );

        statusColumn.setPrefWidth(200);

        historyTable.getColumns().setAll(
                List.of(
                        dateColumn,
                        typeColumn,
                        statusColumn
                )
        );

        historyTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        historyTable.getSelectionModel()
                .setSelectionMode(SelectionMode.SINGLE);

        historyTable.setPlaceholder(
                new Label("Журнал файлових операцій порожній")
        );

        historyTable.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldEntry, newEntry) ->
                                showDetails(newEntry)
                );
    }

    private void configureDetails() {

        configureTextArea(sourceArea);
        configureTextArea(destinationArea);
        configureTextArea(messageArea);
    }

    private void configureTextArea(TextArea area) {

        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(2);
    }

    private void showDetails(
            OperationHistoryEntry entry
    ) {

        if (entry == null) {

            sourceArea.clear();
            destinationArea.clear();
            messageArea.clear();

            return;
        }

        sourceArea.setText(
                formatValue(entry.sourcePath())
        );

        destinationArea.setText(
                formatValue(entry.destinationPath())
        );

        messageArea.setText(
                formatValue(entry.message())
        );

        sourceArea.positionCaret(0);
        destinationArea.positionCaret(0);
        messageArea.positionCaret(0);
    }

    private String formatValue(String value) {

        return value == null || value.isBlank()
                ? "—"
                : value;
    }

    private String formatOperationType(
            FileSystemEventType type
    ) {

        return switch (type) {
            case CREATE -> "Створення";
            case RENAME -> "Перейменування";
            case COPY -> "Копіювання";
            case MOVE -> "Переміщення";
            case DELETE -> "Видалення";
        };
    }

    private String formatStatus(
            OperationStatus status
    ) {

        return switch (status) {
            case SUCCESS -> "Успішно";
            case FAILED -> "Помилка";
            case CANCELLED -> "Скасовано";
            case PARTIAL -> "Виконано частково";
        };
    }

    public void showAndWait() {
        stage.showAndWait();
    }
}
