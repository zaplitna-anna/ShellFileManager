package com.shell.view.dialog;

import com.shell.model.FileSystemItem;
import com.shell.model.SearchMode;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class SearchDialog {

    private final Stage stage = new Stage();

    private final ComboBox<String> queryField =
            new ComboBox<>();

    private final CheckBox exactMatchCheckBox =
            new CheckBox("Точний збіг назви");

    private final Button searchButton =
            new Button("Знайти");

    private final Button cancelButton =
            new Button("Скасувати пошук");

    private final Button navigateButton =
            new Button("Перейти до об'єкта");

    private final Button closeButton =
            new Button("Закрити");

    private final TableView<FileSystemItem> resultsTable =
            new TableView<>();

    private final ProgressIndicator progressIndicator =
            new ProgressIndicator();

    private final Label statusLabel =
            new Label("Введіть пошуковий запит.");

    private boolean running;

    public SearchDialog(
            Window owner,
            Path directory
    ) {
        stage.setTitle("Пошук файлів і каталогів");
        stage.initModality(Modality.APPLICATION_MODAL);

        if (owner != null) {
            stage.initOwner(owner);
        }

        configureTable();
        configureControls();

        Label directoryLabel = new Label(
                "Каталог пошуку: " + directory
        );

        directoryLabel.setWrapText(true);

        queryField.setPromptText(
                "Введіть назву файлу або каталогу"
        );

        HBox searchRow = new HBox(
                10,
                queryField,
                searchButton,
                cancelButton
        );

        searchRow.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(
                queryField,
                Priority.ALWAYS
        );

        VBox searchSection = new VBox(
                10,
                directoryLabel,
                new Label("Пошуковий запит:"),
                searchRow,
                exactMatchCheckBox
        );

        HBox statusRow = new HBox(
                10,
                progressIndicator,
                statusLabel
        );

        statusRow.setAlignment(Pos.CENTER_LEFT);

        HBox buttonRow = new HBox(
                10,
                navigateButton,
                closeButton
        );

        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(
                12,
                searchSection,
                new Separator(),
                resultsTable,
                statusRow,
                buttonRow
        );

        root.setPadding(new Insets(15));

        VBox.setVgrow(
                resultsTable,
                Priority.ALWAYS
        );

        Scene scene = new Scene(root, 850, 520);

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
        stage.setMinHeight(400);

        stage.setOnCloseRequest(event -> {
            if (running) {
                event.consume();
                cancelButton.fire();
            }
        });
        stage.setOnShown(event -> {
            queryField.getEditor().requestFocus();
            showPreviousQueries();
        });

        setRunning(false);
    }

    private void configureTable() {

        TableColumn<FileSystemItem, String> nameColumn =
                new TableColumn<>("Назва");

        nameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getName()
                )
        );

        nameColumn.setPrefWidth(210);

        TableColumn<FileSystemItem, String> typeColumn =
                new TableColumn<>("Тип");

        typeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().isDirectory()
                                ? "Каталог"
                                : "Файл"
                )
        );

        typeColumn.setPrefWidth(100);

        TableColumn<FileSystemItem, String> pathColumn =
                new TableColumn<>("Повний шлях");

        pathColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue()
                                .getPath()
                                .toString()
                )
        );

        pathColumn.setPrefWidth(500);

        resultsTable.getColumns().setAll(
                List.of(
                        nameColumn,
                        typeColumn,
                        pathColumn
                )
        );

        resultsTable.getSelectionModel()
                .setSelectionMode(SelectionMode.SINGLE);

        resultsTable.setPlaceholder(
                new Label("Результати пошуку відсутні")
        );

        resultsTable.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldItem, newItem) ->
                                updateNavigateButton()
                );
    }

    private void configureControls() {

        progressIndicator.setPrefSize(20, 20);
        progressIndicator.setMaxSize(20, 20);

        searchButton.setDefaultButton(true);

        queryField.setEditable(true);
        queryField.setMaxWidth(Double.MAX_VALUE);

        queryField.getEditor().focusedProperty().addListener(
                (observable, oldValue, focused) -> {
                    if (focused) {
                        showPreviousQueries();
                    }
                }
        );

        queryField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER
                    && !queryField.isShowing()
                    && !running) {
                event.consume();
                searchButton.fire();
            }
        });

        closeButton.setOnAction(
                event -> stage.close()
        );
    }

    private void showPreviousQueries() {
        if (stage.isShowing() && !running
                && !queryField.getItems().isEmpty()) {
            queryField.show();
        }
    }

    public void setPreviousQueries(List<String> queries) {
        String currentQuery = getQuery();
        queryField.getItems().setAll(queries);
        setQuery(currentQuery);
    }

    private void updateNavigateButton() {
        navigateButton.setDisable(
                running || getSelectedItem() == null
        );
    }

    public void setRunning(boolean running) {

        this.running = running;

        if (running) {
            queryField.hide();
        }
        queryField.setDisable(running);
        exactMatchCheckBox.setDisable(running);
        searchButton.setDisable(running);

        cancelButton.setDisable(!running);
        closeButton.setDisable(running);

        progressIndicator.setVisible(running);
        progressIndicator.setManaged(running);

        updateNavigateButton();
    }

    public void setResults(
            List<FileSystemItem> results
    ) {
        resultsTable.setItems(
                FXCollections.observableArrayList(results)
        );

        updateNavigateButton();
    }

    public void clearResults() {
        resultsTable.getItems().clear();
        updateNavigateButton();
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    public String getQuery() {
        return queryField.getEditor().getText();
    }

    public SearchMode getSearchMode() {
        return exactMatchCheckBox.isSelected()
                ? SearchMode.EXACT
                : SearchMode.PARTIAL;
    }


    public void setQuery(String query) {
        queryField.setValue(query);
        queryField.getEditor().setText(query);
    }

    public void setSearchMode(SearchMode mode) {
        exactMatchCheckBox.setSelected(
                mode == SearchMode.EXACT
        );
    }


    public FileSystemItem getSelectedItem() {
        return resultsTable.getSelectionModel()
                .getSelectedItem();
    }

    public Button getSearchButton() {
        return searchButton;
    }

    public Button getCancelButton() {
        return cancelButton;
    }

    public Button getNavigateButton() {
        return navigateButton;
    }

    public TableView<FileSystemItem> getResultsTable() {
        return resultsTable;
    }

    public Window getWindow() {
        return stage;
    }

    public void showAndWait() {
        stage.showAndWait();
    }

    public void close() {
        stage.close();
    }
}
