package com.shell.view.dialog;

import com.shell.model.SearchHistoryEntry;
import com.shell.model.SearchMode;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class SearchHistoryDialog {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final Stage stage = new Stage();

    private final TableView<SearchHistoryEntry> historyTable =
            new TableView<>();

    private final Button repeatButton =
            new Button("Повторити пошук");

    private final Button closeButton =
            new Button("Закрити");

    private SearchHistoryEntry selectedEntry;

    public SearchHistoryDialog(
            Window owner,
            List<SearchHistoryEntry> history
    ) {

        stage.setTitle("Історія пошуку");
        stage.initModality(Modality.APPLICATION_MODAL);

        if (owner != null) {
            stage.initOwner(owner);
        }

        configureTable();
        configureControls();

        historyTable.setItems(
                FXCollections.observableArrayList(history)
        );

        Label titleLabel =
                new Label("Раніше виконані пошукові запити");

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
                repeatButton,
                closeButton
        );

        bottomRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(
                12,
                titleLabel,
                historyTable,
                bottomRow
        );

        root.setPadding(new Insets(15));

        VBox.setVgrow(
                historyTable,
                Priority.ALWAYS
        );

        Scene scene = new Scene(root, 900, 450);

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource(
                                "/css/styles.css"
                        ),
                        "Не знайдено файл стилів styles.css."
                ).toExternalForm()
        );

        stage.setScene(scene);
        stage.setMinWidth(700);
        stage.setMinHeight(350);

        stage.setOnShown(event ->
                historyTable.requestFocus()
        );
    }

    private void configureTable() {

        TableColumn<SearchHistoryEntry, String> queryColumn =
                new TableColumn<>("Запит");

        queryColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().query()
                )
        );

        queryColumn.setPrefWidth(180);

        TableColumn<SearchHistoryEntry, String> modeColumn =
                new TableColumn<>("Режим");

        modeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().searchMode()
                                == SearchMode.EXACT
                                ? "Точний"
                                : "Частковий"
                )
        );

        modeColumn.setPrefWidth(110);

        TableColumn<SearchHistoryEntry, String> directoryColumn =
                new TableColumn<>("Каталог");

        directoryColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue()
                                .directory()
                                .toString()
                )
        );

        directoryColumn.setPrefWidth(390);

        TableColumn<SearchHistoryEntry, String> dateColumn =
                new TableColumn<>("Дата й час");

        dateColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue()
                                .createdAt()
                                .format(DATE_FORMATTER)
                )
        );

        dateColumn.setPrefWidth(180);

        historyTable.getColumns().setAll(
                List.of(
                        queryColumn,
                        modeColumn,
                        directoryColumn,
                        dateColumn
                )
        );

        historyTable.getSelectionModel()
                .setSelectionMode(SelectionMode.SINGLE);

        historyTable.setPlaceholder(
                new Label("Історія пошуку порожня")
        );

        historyTable.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldItem, newItem) ->
                                repeatButton.setDisable(
                                        newItem == null
                                )
                );

        historyTable.setRowFactory(table -> {

            TableRow<SearchHistoryEntry> row =
                    new TableRow<>();

            row.setOnMouseClicked(event -> {

                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !row.isEmpty()) {

                    historyTable.getSelectionModel()
                            .select(row.getIndex());

                    selectEntry();
                }
            });

            return row;
        });
    }

    private void configureControls() {

        repeatButton.setDisable(true);
        repeatButton.setDefaultButton(true);

        repeatButton.setOnAction(event ->
                selectEntry()
        );

        closeButton.setOnAction(event ->
                stage.close()
        );
    }

    private void selectEntry() {

        SearchHistoryEntry entry =
                historyTable.getSelectionModel()
                        .getSelectedItem();

        if (entry != null) {
            selectedEntry = entry;
            stage.close();
        }
    }

    public SearchHistoryEntry showAndWait() {

        selectedEntry = null;

        stage.showAndWait();

        return selectedEntry;
    }
}
