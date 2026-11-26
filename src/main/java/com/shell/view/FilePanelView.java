package com.shell.view;

import com.shell.model.FileSystemItem;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class FilePanelView extends VBox {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ComboBox<Path> driveComboBox = new ComboBox<>();
    private final Button parentButton = new Button("↑");
    private final TextField pathField = new TextField();
    private final Button refreshButton = new Button("↻");

    private final TableView<FileSystemItem> fileTable = new TableView<>();
    private final Label statusLabel = new Label("Об'єктів: 0");

    public FilePanelView() {
        configureLayout();
        configureNavigationBar();
        configureTable();
    }

    private void configureLayout() {
        setSpacing(8);
        setPadding(new Insets(10));

        getStyleClass().add("file-panel");
        statusLabel.getStyleClass().add("panel-status");

        VBox.setVgrow(fileTable, Priority.ALWAYS);
    }

    private void configureNavigationBar() {
        driveComboBox.setPrefWidth(85);

        pathField.setPromptText("Шлях до каталогу");

        parentButton.setTooltip(
                new Tooltip("Перейти до батьківського каталогу")
        );

        refreshButton.setTooltip(
                new Tooltip("Оновити вміст каталогу")
        );

        HBox.setHgrow(pathField, Priority.ALWAYS);

        HBox navigationBar = new HBox(
                6,
                driveComboBox,
                parentButton,
                pathField,
                refreshButton
        );

        navigationBar.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
                navigationBar,
                fileTable,
                statusLabel
        );
    }

    private void configureTable() {
        TableColumn<FileSystemItem, String> nameColumn =
                new TableColumn<>("Назва");

        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getName()
                )
        );

        TableColumn<FileSystemItem, String> typeColumn =
                new TableColumn<>("Тип");

        typeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        formatType(cell.getValue())
                )
        );

        TableColumn<FileSystemItem, String> sizeColumn =
                new TableColumn<>("Розмір");

        sizeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        formatSize(cell.getValue().getSize())
                )
        );

        TableColumn<FileSystemItem, String> dateColumn =
                new TableColumn<>("Дата зміни");

        dateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue()
                                .getLastModified()
                                .format(DATE_FORMATTER)
                )
        );

        nameColumn.setPrefWidth(220);
        typeColumn.setPrefWidth(85);
        sizeColumn.setPrefWidth(90);
        dateColumn.setPrefWidth(150);

        // Сортування буде реалізовано через патерн Strategy.
        nameColumn.setSortable(false);
        typeColumn.setSortable(false);
        sizeColumn.setSortable(false);
        dateColumn.setSortable(false);

        fileTable.getColumns().setAll(
                List.of(
                        nameColumn,
                        typeColumn,
                        sizeColumn,
                        dateColumn
                )
        );

        fileTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        fileTable.getSelectionModel()
                .setSelectionMode(SelectionMode.MULTIPLE);

        fileTable.setPlaceholder(
                new Label("Каталог порожній")
        );
    }

    private String formatType(FileSystemItem item) {
        if (item.isDirectory()) {
            return "Каталог";
        }

        if (item.getExtension().isEmpty()) {
            return "Файл";
        }

        return item.getExtension()
                .toUpperCase(Locale.ROOT);
    }

    private String formatSize(long size) {
        if (size < 0) {
            return "—";
        }

        if (size < 1024) {
            return size + " Б";
        }

        if (size < 1024L * 1024) {
            return String.format(
                    Locale.forLanguageTag("uk-UA"),
                    "%.1f КБ",
                    size / 1024.0
            );
        }

        if (size < 1024L * 1024 * 1024) {
            return String.format(
                    Locale.forLanguageTag("uk-UA"),
                    "%.1f МБ",
                    size / (1024.0 * 1024)
            );
        }

        return String.format(
                Locale.forLanguageTag("uk-UA"),
                "%.1f ГБ",
                size / (1024.0 * 1024 * 1024)
        );
    }

    public void setAvailableDrives(List<Path> drives) {
        driveComboBox.getItems().setAll(drives);
    }

    public void setCurrentPath(Path path) {
        pathField.setText(path.toString());
    }

    public void setItems(List<FileSystemItem> items) {
        fileTable.getItems().setAll(items);
        statusLabel.setText("Об'єктів: " + items.size());
    }

    public List<FileSystemItem> getSelectedItems() {
        return List.copyOf(
                fileTable.getSelectionModel().getSelectedItems()
        );
    }

    public ComboBox<Path> getDriveComboBox() {
        return driveComboBox;
    }

    public Button getParentButton() {
        return parentButton;
    }

    public TextField getPathField() {
        return pathField;
    }

    public Button getRefreshButton() {
        return refreshButton;
    }

    public TableView<FileSystemItem> getFileTable() {
        return fileTable;
    }
}
