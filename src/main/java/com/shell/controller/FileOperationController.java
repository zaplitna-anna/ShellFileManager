package com.shell.controller;

import com.shell.model.FileSystemItem;
import com.shell.model.FileSystemItemType;
import com.shell.service.FileOperationService;
import com.shell.service.NavigationService;
import com.shell.view.MainView;
import com.shell.view.dialog.AlertDialogs;
import com.shell.view.dialog.FileOperationDialogs;

import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class FileOperationController {
    private final MainView view;
    private final FileOperationService fileOperationService;
    private final BackgroundOperationExecutor executor;
    private final AlertDialogs alerts;
    private final Supplier<FilePanelController> activePanelSupplier;
    private final Supplier<FilePanelController> passivePanelSupplier;
    private final Supplier<Window> ownerSupplier;

    public FileOperationController(
            MainView view,
            FileOperationService fileOperationService,
            BackgroundOperationExecutor executor,
            AlertDialogs alerts,
            Supplier<FilePanelController> activePanelSupplier,
            Supplier<FilePanelController> passivePanelSupplier,
            Supplier<Window> ownerSupplier
    ) {
        this.view = Objects.requireNonNull(view);
        this.fileOperationService = Objects.requireNonNull(fileOperationService);
        this.executor = Objects.requireNonNull(executor);
        this.alerts = Objects.requireNonNull(alerts);
        this.activePanelSupplier = Objects.requireNonNull(activePanelSupplier);
        this.passivePanelSupplier = Objects.requireNonNull(passivePanelSupplier);
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier);
    }

    public void handleCreate() {
        if (executor.isOperationRunning()) {
            return;
        }
        Path directory =
                activePanelSupplier.get().getCurrentDirectory();

        if (directory == null) {
            alerts.showWarning(
                    "Створення об'єкта",
                    "Спочатку відкрийте каталог."
            );

            return;
        }

        FileOperationDialogs.showCreateDialog(ownerSupplier.get())
                .ifPresent(result -> {
                    try {
                        if (result.type()
                                == FileSystemItemType.DIRECTORY) {

                            fileOperationService.createDirectory(
                                    directory,
                                    result.name()
                            );

                        } else {

                            fileOperationService.createFile(
                                    directory,
                                    result.name()
                            );
                        }

                        view.setStatus(
                                "Створено об'єкт: "
                                        + result.name()
                        );

                    } catch (IOException
                             | SecurityException
                             | InvalidPathException e) {

                        alerts.showOperationError(
                                "Не вдалося створити об'єкт",
                                e
                        );
                        view.setStatus(
                                "Не вдалося створити об'єкт: "
                                        + result.name()
                        );
                    }
                });
    }

    public void handleRename() {
        if (executor.isOperationRunning()) {
            return;
        }

        List<FileSystemItem> selectedItems =
                activePanelSupplier.get().getSelectedItems();

        if (selectedItems.size() != 1) {
            alerts.showWarning(
                    "Перейменування",
                    "Виберіть один файл або каталог."
            );

            return;
        }

        FileSystemItem item = selectedItems.get(0);

        FileOperationDialogs.showRenameDialog(
                ownerSupplier.get(),
                item
        ).ifPresent(newName -> {

            if (item.getName().equals(newName)) {
                return;
            }

            try {
                fileOperationService.rename(
                        item.getPath(),
                        newName
                );

                view.setStatus(
                        "Перейменовано: "
                                + item.getName()
                                + " → "
                                + newName
                );

            } catch (IOException
                     | SecurityException
                     | InvalidPathException e) {

                alerts.showOperationError(
                        "Не вдалося перейменувати об'єкт",
                        e
                );
                view.setStatus(
                        "Не вдалося перейменувати об'єкт: "
                                + item.getName()
                );
            }
        });
    }

    public void handleCopy() {
        if (executor.isOperationRunning()) {
            return;
        }
        FilePanelController sourceController =
                activePanelSupplier.get();

        FilePanelController destinationController =
                passivePanelSupplier.get();

        List<FileSystemItem> selectedItems =
                sourceController.getSelectedItems();

        if (selectedItems.isEmpty()) {
            alerts.showWarning(
                    "Копіювання",
                    "Виберіть файли або каталоги "
                            + "для копіювання."
            );

            return;
        }

        Path sourceDirectory =
                sourceController.getCurrentDirectory();

        Path destinationDirectory =
                destinationController.getCurrentDirectory();

        if (sourceDirectory == null
                || destinationDirectory == null) {

            alerts.showWarning(
                    "Копіювання",
                    "Не вдалося визначити каталог "
                            + "джерела або призначення."
            );

            return;
        }

        List<Path> sourcePaths = selectedItems.stream()
                .map(FileSystemItem::getPath)
                .toList();

        boolean confirmed =
                FileOperationDialogs.showCopyConfirmation(
                        ownerSupplier.get(),
                        selectedItems,
                        destinationDirectory
                );


        if (!confirmed) {
            return;
        }


        executor.executeInBackground(
                context -> fileOperationService.copy(
                        sourcePaths,
                        destinationDirectory,
                        context
                ),
                "Копіювання об'єктів",
                "Виконується копіювання...",
                "Скопійовано об'єктів: "
                        + sourcePaths.size(),
                "Не вдалося завершити копіювання",
                "Копіювання не завершено. "
                        + "Частину об'єктів могло бути скопійовано."
        );
    }

    public void handleMove() {
        if (executor.isOperationRunning()) {
            return;
        }

        FilePanelController sourceController =
                activePanelSupplier.get();

        FilePanelController destinationController =
                passivePanelSupplier.get();

        List<FileSystemItem> selectedItems =
                sourceController.getSelectedItems();

        if (selectedItems.isEmpty()) {
            alerts.showWarning(
                    "Переміщення",
                    "Виберіть файли або каталоги "
                            + "для переміщення."
            );

            return;
        }

        Path sourceDirectory =
                sourceController.getCurrentDirectory();

        Path destinationDirectory =
                destinationController.getCurrentDirectory();

        if (sourceDirectory == null
                || destinationDirectory == null) {

            alerts.showWarning(
                    "Переміщення",
                    "Не вдалося визначити каталог "
                            + "джерела або призначення."
            );

            return;
        }

        List<Path> sourcePaths = selectedItems.stream()
                .map(FileSystemItem::getPath)
                .toList();

        boolean confirmed =
                FileOperationDialogs.showMoveConfirmation(
                        ownerSupplier.get(),
                        selectedItems,
                        destinationDirectory
                );

        if (!confirmed) {
            return;
        }


        executor.executeInBackground(
                context -> fileOperationService.move(
                        sourcePaths,
                        destinationDirectory,
                        context
                ),
                "Переміщення об'єктів",
                "Виконується переміщення...",
                "Переміщено об'єктів: "
                        + sourcePaths.size(),
                "Не вдалося завершити переміщення",
                "Переміщення не завершено. "
                        + "Частину об'єктів могло бути переміщено."
        );
    }

    public void handleDelete() {
        if (executor.isOperationRunning()) {
            return;
        }

        FilePanelController controller =
                activePanelSupplier.get();

        List<FileSystemItem> selectedItems =
                controller.getSelectedItems();

        if (selectedItems.isEmpty()) {
            alerts.showWarning(
                    "Видалення",
                    "Виберіть файли або каталоги для видалення."
            );

            return;
        }

        List<Path> paths = selectedItems.stream()
                .map(FileSystemItem::getPath)
                .toList();

        boolean confirmed =
                FileOperationDialogs.showDeleteConfirmation(
                        ownerSupplier.get(),
                        selectedItems
                );

        if (!confirmed) {
            return;
        }


        executor.executeInBackground(
                context -> fileOperationService.delete(
                        paths,
                        context
                ),
                "Видалення об'єктів",
                "Виконується видалення...",
                "Видалено об'єктів: "
                        + paths.size(),
                "Не вдалося завершити видалення",
                "Видалення не завершено. "
                        + "Частину об'єктів могло бути видалено."
        );
    }

    public void handleOpen(FileSystemItem item) {
        if (executor.isOperationRunning()) {
            return;
        }
        try {
            fileOperationService.open(item.getPath());

            view.setStatus(
                    "Запит на відкриття файлу: "
                            + item.getName()
            );

        } catch (IOException
                 | SecurityException
                 | IllegalArgumentException
                 | UnsupportedOperationException e) {

            alerts.showOperationError(
                    "Не вдалося відкрити файл",
                    e
            );

            view.setStatus(
                    "Не вдалося відкрити файл: "
                            + item.getName()
            );
        }
    }
}
