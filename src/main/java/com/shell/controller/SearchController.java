package com.shell.controller;

import com.shell.model.FileSystemItem;
import com.shell.model.SearchMode;
import com.shell.operation.CancellationToken;
import com.shell.service.SearchService;
import com.shell.view.dialog.AlertDialogs;
import com.shell.view.dialog.SearchDialog;
import com.shell.model.SearchHistoryEntry;
import com.shell.view.dialog.SearchHistoryDialog;

import java.sql.SQLException;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.TableRow;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchController {

    private static final Logger LOGGER =
            Logger.getLogger(
                    SearchController.class.getName()
            );

    private final SearchService searchService;

    private final Supplier<FilePanelController>
            activeControllerSupplier;

    private final Supplier<Window> ownerSupplier;

    private CancellationToken cancellationToken;

    public SearchController(
            SearchService searchService,
            Supplier<FilePanelController>
                    activeControllerSupplier,
            Supplier<Window> ownerSupplier
    ) {
        this.searchService = Objects.requireNonNull(
                searchService,
                "Сервіс пошуку не може бути null."
        );

        this.activeControllerSupplier =
                Objects.requireNonNull(
                        activeControllerSupplier,
                        "Постачальник активної панелі "
                                + "не може бути null."
                );

        this.ownerSupplier = Objects.requireNonNull(
                ownerSupplier,
                "Постачальник вікна не може бути null."
        );
    }


    public void handleSearch() {

        FilePanelController panel =
                activeControllerSupplier.get();

        Path directory =
                panel.getCurrentDirectory();

        if (directory == null) {
            showError(
                    ownerSupplier.get(),
                    "Не визначено каталог пошуку."
            );

            return;
        }

        showSearchDialog(
                panel,
                directory,
                "",
                SearchMode.PARTIAL
        );
    }


    public void handleSearchHistory() {

        try {
            List<SearchHistoryEntry> history =
                    searchService.getSearchHistory();

            SearchHistoryDialog historyDialog =
                    new SearchHistoryDialog(
                            ownerSupplier.get(),
                            history
                    );

            SearchHistoryEntry entry =
                    historyDialog.showAndWait();

            if (entry == null) {
                return;
            }

            FilePanelController panel =
                    activeControllerSupplier.get();

            Path directory = panel.getCurrentDirectory();

            if (directory == null) {
                showError(
                        ownerSupplier.get(),
                        "Не визначено каталог пошуку."
                );
                return;
            }

            showSearchDialog(
                    panel,
                    directory,
                    entry.query(),
                    entry.searchMode()
            );

        } catch (SQLException exception) {

            LOGGER.log(
                    Level.WARNING,
                    "Не вдалося отримати історію пошуку.",
                    exception
            );

            showError(
                    ownerSupplier.get(),
                    "Не вдалося завантажити історію пошуку."
            );
        }
    }


    private void showSearchDialog(
            FilePanelController panel,
            Path directory,
            String query,
            SearchMode mode
    ) {

        SearchDialog dialog = new SearchDialog(
                ownerSupplier.get(),
                directory
        );

        loadPreviousQueries(dialog);
        dialog.setQuery(query);
        dialog.setSearchMode(mode);

        dialog.getSearchButton().setOnAction(
                event -> startSearch(
                        dialog,
                        directory
                )
        );

        dialog.getCancelButton().setOnAction(
                event -> cancelSearch(dialog)
        );

        dialog.getNavigateButton().setOnAction(
                event -> navigateToResult(
                        dialog,
                        panel
                )
        );

        dialog.getResultsTable().setRowFactory(
                table -> {

                    TableRow<FileSystemItem> row =
                            new TableRow<>();

                    row.setOnMouseClicked(event -> {

                        if (event.getButton()
                                == MouseButton.PRIMARY
                                && event.getClickCount() == 2
                                && !row.isEmpty()
                                && cancellationToken == null) {

                            dialog.getResultsTable()
                                    .getSelectionModel()
                                    .select(row.getIndex());

                            navigateToResult(
                                    dialog,
                                    panel
                            );
                        }
                    });

                    return row;
                }
        );

        dialog.showAndWait();
    }


    private void loadPreviousQueries(SearchDialog dialog) {
        try {
            List<String> queries = searchService.getSearchHistory()
                    .stream()
                    .map(SearchHistoryEntry::query)
                    .distinct()
                    .toList();

            dialog.setPreviousQueries(queries);

        } catch (SQLException exception) {
            LOGGER.log(
                    Level.WARNING,
                    "Не вдалося завантажити попередні запити.",
                    exception
            );
            showError(
                    dialog.getWindow().isShowing()
                            ? dialog.getWindow() : ownerSupplier.get(),
                    "Не вдалося завантажити попередні запити. "
                            + "Новий пошук залишається доступним."
            );
        }
    }

    private void startSearch(
            SearchDialog dialog,
            Path directory
    ) {

        if (cancellationToken != null) {
            return;
        }

        String query = dialog.getQuery();

        if (query == null || query.isBlank()) {
            dialog.setStatus(
                    "Введіть пошуковий запит."
            );

            return;
        }

        SearchMode mode = dialog.getSearchMode();

        CancellationToken token =
                new CancellationToken();

        cancellationToken = token;

        Task<List<FileSystemItem>> task =
                new Task<>() {

                    @Override
                    protected List<FileSystemItem> call()
                            throws Exception {

                        return searchService.search(
                                directory,
                                query,
                                mode,
                                token
                        );
                    }
                };

        task.setOnSucceeded(event -> {

            finishSearch(dialog);

            List<FileSystemItem> results =
                    task.getValue();

            dialog.setResults(results);
            loadPreviousQueries(dialog);

            if (results.isEmpty()) {
                dialog.setStatus(
                        "Відповідних об'єктів не знайдено."
                );
            } else {
                dialog.setStatus(
                        "Знайдено об'єктів: "
                                + results.size()
                );
            }
        });

        task.setOnFailed(event -> {

            finishSearch(dialog);

            Throwable exception =
                    task.getException();

            if (exception instanceof CancellationException) {
                dialog.setStatus("Пошук скасовано.");
                return;
            }

            LOGGER.log(
                    Level.WARNING,
                    "Помилка під час пошуку.",
                    exception
            );

            dialog.setStatus(
                    "Не вдалося завершити пошук."
            );

            String message =
                    exception == null
                            || exception.getMessage() == null
                            ? "Виникла помилка під час пошуку."
                            : exception.getMessage();

            showError(
                    dialog.getWindow(),
                    message
            );
        });

        dialog.clearResults();
        dialog.setRunning(true);

        dialog.setStatus(
                "Виконується пошук..."
        );

        try {
            Thread thread = new Thread(
                    task,
                    "shell-search"
            );

            thread.setDaemon(true);
            thread.start();

        } catch (RuntimeException exception) {

            finishSearch(dialog);

            LOGGER.log(
                    Level.WARNING,
                    "Не вдалося запустити пошук.",
                    exception
            );

            dialog.setStatus(
                    "Не вдалося запустити пошук."
            );

            showError(
                    dialog.getWindow(),
                    "Не вдалося запустити пошук."
            );
        }
    }

    private void cancelSearch(SearchDialog dialog) {

        if (cancellationToken == null) {
            return;
        }

        cancellationToken.cancel();

        dialog.getCancelButton().setDisable(true);

        dialog.setStatus(
                "Скасування пошуку..."
        );
    }

    private void finishSearch(SearchDialog dialog) {

        cancellationToken = null;
        dialog.setRunning(false);
    }

    private void navigateToResult(
            SearchDialog dialog,
            FilePanelController panel
    ) {

        if (cancellationToken != null) {
            return;
        }

        FileSystemItem item =
                dialog.getSelectedItem();

        if (item == null) {
            return;
        }

        Path path = item.getPath();

        dialog.close();

        panel.navigateToItem(path);
    }

    private void showError(
            Window owner,
            String message
    ) {
        AlertDialogs alerts = new AlertDialogs(() -> owner);

        alerts.showAlert(
                Alert.AlertType.ERROR,
                "Помилка",
                "Помилка пошуку",
                message
        );
    }
}
