package com.shell.view;

import com.shell.model.FileSystemItem;
import com.shell.strategy.sorting.*;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

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

import com.shell.icons.FileIconProvider;

public class FilePanelView extends VBox {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ComboBox<Path> driveComboBox = new ComboBox<>();
    private final Button parentButton = new Button("↑");
    private final TextField pathField = new TextField();
    private final Button refreshButton = new Button("↻");

    private final TableView<FileSystemItem> fileTable = new TableView<>();

    private final TableColumn<FileSystemItem, String> nameColumn =
            new TableColumn<>("Назва");
    private final TableColumn<FileSystemItem, String> typeColumn =
            new TableColumn<>("Тип");
    private final TableColumn<FileSystemItem, String> sizeColumn =
            new TableColumn<>("Розмір");
    private final TableColumn<FileSystemItem, String> dateColumn =
            new TableColumn<>("Дата зміни");

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
        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getName()
                )
        );

        nameColumn.setCellFactory(column ->
                new TableCell<FileSystemItem, String>() {

                    @Override
                    protected void updateItem(
                            String name,
                            boolean empty
                    ) {
                        super.updateItem(name, empty);

                        FileSystemItem item =
                                getTableRow() == null
                                        ? null
                                        : getTableRow().getItem();

                        if (empty || name == null || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(name);
                            setGraphic(
                                    FileIconProvider.createIcon(item)
                            );
                            setGraphicTextGap(7);
                        }
                    }
                }
        );

        typeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        formatType(cell.getValue())
                )
        );

        sizeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        formatSize(cell.getValue().getSize())
                )
        );

        dateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        DATE_FORMATTER.format(
                                cell.getValue().getLastModified()
                        )
                )
        );

        nameColumn.setPrefWidth(220);
        typeColumn.setPrefWidth(85);
        sizeColumn.setPrefWidth(90);
        dateColumn.setPrefWidth(150);

        nameColumn.setSortable(true);
        typeColumn.setSortable(true);
        sizeColumn.setSortable(true);
        dateColumn.setSortable(true);

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

        fileTable.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE
        );

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

    public TableColumn<FileSystemItem, String> getNameColumn() {
        return nameColumn;
    }

    public TableColumn<FileSystemItem, String> getTypeColumn() {
        return typeColumn;
    }

    public TableColumn<FileSystemItem, String> getSizeColumn() {
        return sizeColumn;
    }

    public TableColumn<FileSystemItem, String> getDateColumn() {
        return dateColumn;
    }
}
