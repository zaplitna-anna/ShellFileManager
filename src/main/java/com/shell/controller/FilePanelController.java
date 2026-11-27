package com.shell.controller;

import com.shell.model.FileSystemItem;
import com.shell.service.NavigationService;
import com.shell.view.FilePanelView;
import com.shell.strategy.sorting.*;
import com.shell.observer.FileSystemEvent;
import com.shell.observer.FileSystemObserver;

import javafx.application.Platform;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.scene.control.TableRow;
import javafx.scene.input.MouseButton;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.function.Consumer;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class FilePanelController
        implements FileSystemObserver {

    private final FilePanelView view;
    private final NavigationService navigationService;

    private final FileSortingContext sortingContext =
            new FileSortingContext();

    private Path currentDirectory;
    private Consumer<FileSystemItem> onFileOpen = item -> {
    };
    private boolean updatingDriveSelection;

    public FilePanelController(
            FilePanelView view,
            NavigationService navigationService
    ) {
        this.view = Objects.requireNonNull(
                view,
                "Файлова панель не може бути null."
        );

        this.navigationService = Objects.requireNonNull(
                navigationService,
                "Сервіс навігації не може бути null."
        );

        configureEventHandlers();
        configureSorting();
    }

    public void initialize() {
        view.setAvailableDrives(
                navigationService.getAvailableDrives()
        );

        navigateTo(navigationService.getInitialDirectory());
    }

    private void configureEventHandlers() {
        view.getParentButton().setOnAction(event ->
                navigateToParent()
        );

        view.getRefreshButton().setOnAction(event ->
                refresh()
        );

        view.getPathField().setOnAction(event ->
                navigateFromPathField()
        );

        view.getDriveComboBox().setOnAction(event -> {
            if (updatingDriveSelection) {
                return;
            }

            Path selectedDrive =
                    view.getDriveComboBox().getValue();

            if (selectedDrive != null) {
                navigateTo(selectedDrive);
            }
        });


        view.getFileTable().setRowFactory(tableView -> {
            TableRow<FileSystemItem> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !row.isEmpty()) {

                    openItem(row.getItem());
                }
            });

            return row;
        });


        view.getFileTable().addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> {
                    if (event.getCode() == KeyCode.ENTER
                            && !event.isControlDown()
                            && !event.isAltDown()
                            && !event.isShiftDown()
                            && !event.isMetaDown()) {

                        event.consume();

                        List<FileSystemItem> selectedItems =
                                view.getSelectedItems();

                        if (selectedItems.size() == 1) {
                            openItem(selectedItems.getFirst());
                        }
                    }
                }
        );
    }


    private void configureSorting() {
        TableView<FileSystemItem> table = view.getFileTable();

        table.setSortPolicy(currentTable -> {

            if (currentTable.getSortOrder().isEmpty()) {
                return true;
            }

            TableColumn<FileSystemItem, ?> column =
                    currentTable.getSortOrder().get(0);

            if (column == view.getNameColumn()) {
                sortingContext.setStrategy(
                        new NameSortingStrategy()
                );

            } else if (column == view.getTypeColumn()) {
                sortingContext.setStrategy(
                        new TypeSortingStrategy()
                );

            } else if (column == view.getSizeColumn()) {
                sortingContext.setStrategy(
                        new SizeSortingStrategy()
                );

            } else if (column == view.getDateColumn()) {
                sortingContext.setStrategy(
                        new DateSortingStrategy()
                );

            } else {
                return false;
            }

            sortingContext.setAscending(
                    column.getSortType()
                            == TableColumn.SortType.ASCENDING
            );

            List<FileSystemItem> sortedItems =
                    sortingContext.sort(
                            new ArrayList<>(currentTable.getItems())
                    );

            currentTable.getItems().setAll(sortedItems);

            return true;
        });

        view.getNameColumn().setSortType(
                TableColumn.SortType.ASCENDING
        );

        table.getSortOrder().setAll(
                List.of(view.getNameColumn())
        );
    }

    public void navigateTo(Path directory) {
        try {
            if (directory == null) {
                throw new IOException("Шлях до каталогу не вказано.");
            }

            Path realPath =
                    navigationService.getRealDirectoryPath(directory);

            List<FileSystemItem> items =
                    navigationService.getDirectoryContents(
                            realPath
                    );

            currentDirectory = realPath;

            view.setCurrentPath(currentDirectory);
            view.setItems(
                    sortingContext.sort(items)
            );

            updateNavigationControls();

        } catch (AccessDeniedException | SecurityException e) {
            restoreNavigationControls();

            showError(
                    "Не вдалося відкрити каталог",
                    "Немає прав доступу до каталогу: " + directory
            );

        } catch (NoSuchFileException e) {
            restoreNavigationControls();

            showError(
                    "Не вдалося відкрити каталог",
                    "Каталог не існує: " + directory
            );

        } catch (IOException e) {
            restoreNavigationControls();

            showError(
                    "Не вдалося відкрити каталог",
                    e.getClass() == IOException.class
                            && e.getMessage() != null
                            ? e.getMessage()
                            : "Не вдалося прочитати вміст каталогу: "
                            + directory
            );
        }
    }

    public void navigateToParent() {
        if (currentDirectory == null) {
            return;
        }

        Path parent = navigationService.getParentDirectory(
                currentDirectory
        );

        if (!parent.equals(currentDirectory)) {
            navigateTo(parent);
        }
    }

    private void navigateFromPathField() {
        String input = view.getPathField().getText().strip();

        if (input.isEmpty()) {
            restoreNavigationControls();

            showError(
                    "Некоректний шлях",
                    "Введіть шлях до каталогу."
            );

            return;
        }

        try {
            Path path = Path.of(input);

            if (!path.isAbsolute() && currentDirectory != null) {
                path = currentDirectory.resolve(path);
            }

            navigateTo(path);

        } catch (InvalidPathException e) {
            restoreNavigationControls();

            showError(
                    "Некоректний шлях",
                    "Введений шлях має некоректний формат."
            );
        }
    }

    public void refresh() {
        if (currentDirectory != null) {
            navigateTo(currentDirectory);
        }
    }

    private void updateNavigationControls() {
        updatingDriveSelection = true;

        try {
            Path root = currentDirectory == null
                    ? null
                    : currentDirectory.getRoot();

            view.getDriveComboBox().setValue(root);

            view.getParentButton().setDisable(
                    currentDirectory == null
                            || currentDirectory.getParent() == null
            );

        } finally {
            updatingDriveSelection = false;
        }
    }

    private void restoreNavigationControls() {
        if (currentDirectory != null) {
            view.setCurrentPath(currentDirectory);
        } else {
            view.getPathField().clear();
        }

        updateNavigationControls();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle("Помилка");
        alert.setHeaderText(title);
        alert.setContentText(message);

        if (view.getScene() != null
                && view.getScene().getWindow() != null) {
            alert.initOwner(view.getScene().getWindow());
        }

        alert.showAndWait();
    }

    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    public void setOnFileOpen(
            Consumer<FileSystemItem> onFileOpen
    ) {
        this.onFileOpen = Objects.requireNonNull(
                onFileOpen,
                "Обробник відкриття файлу не може бути null."
        );
    }

    public void openItem(FileSystemItem item) {
        if (item == null) {
            return;
        }

        if (item.isDirectory()) {
            navigateTo(item.getPath());
        } else {
            onFileOpen.accept(item);
        }
    }


    @Override
    public void onFileSystemChanged(FileSystemEvent event) {
        if (Platform.isFxApplicationThread()) {
            handleFileSystemEvent(event);
        } else {
            Platform.runLater(
                    () -> handleFileSystemEvent(event)
            );
        }
    }


    private void handleFileSystemEvent(FileSystemEvent event) {
        if (currentDirectory == null) {
            return;
        }

        switch (event.type()) {

            case CREATE, COPY -> handleDirectoryContentChanged(event);

            case RENAME -> handleRenameEvent(event);

            case DELETE, MOVE -> handleRemoveOrMoveEvent(event);
        }
    }


    private void handleDirectoryContentChanged(
            FileSystemEvent event
    ) {
        if (currentDirectory.equals(
                event.destinationDirectory()
        )) {
            refresh();
        }
    }


    private void handleRenameEvent(FileSystemEvent event) {
        if (event.affectedPaths().isEmpty()
                || event.newPath() == null) {
            return;
        }

        Path oldPath = event.affectedPaths().getFirst();
        Path newPath = event.newPath();

        if (currentDirectory.startsWith(oldPath)) {

            Path relativePath =
                    oldPath.relativize(currentDirectory);

            navigateTo(newPath.resolve(relativePath));

        } else if (currentDirectory.equals(
                event.sourceDirectory()
        ) || currentDirectory.equals(
                event.destinationDirectory()
        )) {

            refresh();
        }
    }


    private void handleRemoveOrMoveEvent(
            FileSystemEvent event
    ) {
        boolean affected =
                currentDirectory.equals(
                        event.sourceDirectory()
                )
                        || currentDirectory.equals(
                        event.destinationDirectory()
                )
                        || event.affectedPaths().stream()
                        .anyMatch(currentDirectory::startsWith);

        if (affected) {
            refreshOrNavigateToAvailableDirectory();
        }
    }


    private void refreshOrNavigateToAvailableDirectory() {
        if (currentDirectory == null) {
            return;
        }

        if (navigationService.isAccessibleDirectory(
                currentDirectory
        )) {
            refresh();
            return;
        }

        Path availableDirectory =
                currentDirectory.getParent();

        while (availableDirectory != null) {

            if (navigationService.isAccessibleDirectory(
                    availableDirectory
            )) {
                navigateTo(availableDirectory);
                return;
            }

            availableDirectory =
                    availableDirectory.getParent();
        }

        showError(
                "Оновлення файлової панелі",
                "Не вдалося знайти доступний каталог."
        );
    }


    public List<FileSystemItem> getSelectedItems() {
        return view.getSelectedItems();
    }
}
