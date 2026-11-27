package com.shell.controller;

import com.shell.service.NavigationService;
import com.shell.view.FilePanelView;
import com.shell.view.MainView;
import com.shell.model.FileSystemItem;
import com.shell.model.FileSystemItemType;
import com.shell.service.FileOperationService;
import com.shell.view.dialog.FileOperationDialogs;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.beans.value.ChangeListener;

import java.util.Objects;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.DirectoryNotEmptyException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainController {
    private static final Logger LOGGER =
            Logger.getLogger(MainController.class.getName());

    private static final PseudoClass ACTIVE_PANEL =
            PseudoClass.getPseudoClass("active");

    private final MainView view;
    private final FileOperationService fileOperationService;

    private final FilePanelController leftController;
    private final FilePanelController rightController;

    private FilePanelController activeController;

    private final ChangeListener<Node> focusListener =
            (observable, oldNode, newNode) ->
                    updateActivePanelByFocus(newNode);


    public MainController(
            MainView view,
            NavigationService navigationService,
            FileOperationService fileOperationService
    ) {
        this.view = Objects.requireNonNull(
                view,
                "Головне представлення не може бути null."
        );

        Objects.requireNonNull(
                navigationService,
                "Сервіс навігації не може бути null."
        );

        this.fileOperationService = Objects.requireNonNull(
                fileOperationService,
                "Сервіс файлових операцій не може бути null."
        );

        leftController = new FilePanelController(
                view.getLeftPanel(),
                navigationService
        );

        rightController = new FilePanelController(
                view.getRightPanel(),
                navigationService
        );

        leftController.setOnFileOpen(this::handleOpen);
        rightController.setOnFileOpen(this::handleOpen);

        activeController = leftController;

        configurePanelEvents();
        configureMenuEvents();
        configureKeyboardEvents();
        configureOperationEvents();
    }

    public void initialize() {
        leftController.initialize();
        rightController.initialize();

        setActivePanel(leftController);

        Platform.runLater(() ->
                view.getLeftPanel()
                        .getFileTable()
                        .requestFocus()
        );
    }

    private void configurePanelEvents() {
        view.getLeftPanel().addEventFilter(
                MouseEvent.MOUSE_PRESSED,
                event -> setActivePanel(leftController)
        );

        view.getRightPanel().addEventFilter(
                MouseEvent.MOUSE_PRESSED,
                event -> setActivePanel(rightController)
        );

        view.sceneProperty().addListener(
                (observable, oldScene, newScene) -> {

                    if (oldScene != null) {
                        oldScene.focusOwnerProperty()
                                .removeListener(focusListener);
                    }

                    if (newScene != null) {
                        newScene.focusOwnerProperty()
                                .addListener(focusListener);

                        updateActivePanelByFocus(
                                newScene.getFocusOwner()
                        );
                    }
                }
        );

        if (view.getScene() != null) {
            view.getScene().focusOwnerProperty()
                    .addListener(focusListener);

            updateActivePanelByFocus(
                    view.getScene().getFocusOwner()
            );
        }
    }

    private void updateActivePanelByFocus(Node node) {
        if (belongsToPanel(node, view.getLeftPanel())) {
            setActivePanel(leftController);
        } else if (belongsToPanel(node, view.getRightPanel())) {
            setActivePanel(rightController);
        }
    }

    private boolean belongsToPanel(
            Node node,
            FilePanelView panel
    ) {
        while (node != null) {
            if (node == panel) {
                return true;
            }

            node = node.getParent();
        }

        return false;
    }

    private void configureMenuEvents() {
        view.getExitMenuItem().setOnAction(
                event -> Platform.exit()
        );
    }

    private void configureOperationEvents() {
        view.getCreateButton().setOnAction(
                event -> handleCreate()
        );

        view.getCreateMenuItem().setOnAction(
                event -> handleCreate()
        );

        view.getRenameButton().setOnAction(
                event -> handleRename()
        );

        view.getRenameMenuItem().setOnAction(
                event -> handleRename()
        );

        view.getDeleteButton().setOnAction(
                event -> handleDelete()
        );

        view.getDeleteMenuItem().setOnAction(
                event -> handleDelete()
        );
    }

    private void configureKeyboardEvents() {
        view.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            if (event.getCode() == KeyCode.TAB
                    && !event.isControlDown()
                    && !event.isAltDown()
                    && !event.isShiftDown()
                    && !event.isMetaDown()) {

                switchActivePanel();
                event.consume();

            } else if (event.getCode() == KeyCode.F2
                    && !event.isControlDown()
                    && !event.isAltDown()
                    && !event.isShiftDown()
                    && !event.isMetaDown()) {

                event.consume();
                handleRename();
            }
        });
    }


    private void handleCreate() {
        Path directory =
                getActiveController().getCurrentDirectory();

        if (directory == null) {
            showWarning(
                    "Створення об'єкта",
                    "Спочатку відкрийте каталог."
            );

            return;
        }

        FileOperationDialogs.showCreateDialog(getOwner())
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

                        refreshAffectedPanels(directory);

                        view.setStatus(
                                "Створено об'єкт: "
                                        + result.name()
                        );

                    } catch (IOException
                             | SecurityException
                             | InvalidPathException e) {

                        showOperationError(
                                "Не вдалося створити об'єкт",
                                e
                        );
                    }
                });
    }


    private void handleRename() {
        List<FileSystemItem> selectedItems =
                getActiveController().getSelectedItems();

        if (selectedItems.size() != 1) {
            showWarning(
                    "Перейменування",
                    "Виберіть один файл або каталог."
            );

            return;
        }

        FileSystemItem item = selectedItems.get(0);

        FileOperationDialogs.showRenameDialog(
                getOwner(),
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

                Path oldPath = item.getPath();

                Path newPath = oldPath.resolveSibling(newName);

                updatePanelsAfterRename(oldPath, newPath);

                view.setStatus(
                        "Перейменовано: "
                                + item.getName()
                                + " → "
                                + newName
                );

            } catch (IOException
                     | SecurityException
                     | InvalidPathException e) {

                showOperationError(
                        "Не вдалося перейменувати об'єкт",
                        e
                );
            }
        });
    }


    private void handleDelete() {
        FilePanelController controller =
                getActiveController();

        List<FileSystemItem> selectedItems =
                controller.getSelectedItems();

        if (selectedItems.isEmpty()) {
            showWarning(
                    "Видалення",
                    "Виберіть файли або каталоги для видалення."
            );

            return;
        }

        Path sourceDirectory =
                controller.getCurrentDirectory();

        List<Path> paths = selectedItems.stream()
                .map(FileSystemItem::getPath)
                .toList();

        boolean confirmed =
                FileOperationDialogs.showDeleteConfirmation(
                        getOwner(),
                        selectedItems
                );

        if (!confirmed) {
            return;
        }

        try {
            fileOperationService.delete(paths);

            view.setStatus(
                    "Видалено об'єктів: " + paths.size()
            );

        } catch (IOException
                 | SecurityException
                 | InvalidPathException e) {

            showOperationError(
                    "Не вдалося завершити видалення",
                    e
            );

            view.setStatus(
                    "Видалення не завершено. "
                            + "Частину об'єктів могло бути видалено."
            );

        } finally {
            updatePanelsAfterDelete(
                    sourceDirectory,
                    paths
            );
        }
    }


    private void handleOpen(FileSystemItem item) {
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

            showOperationError(
                    "Не вдалося відкрити файл",
                    e
            );

            view.setStatus(
                    "Не вдалося відкрити файл: "
                            + item.getName()
            );
        }
    }


    private void refreshAffectedPanels(Path directory) {
        if (directory == null) {
            return;
        }

        Path normalizedDirectory =
                directory.toAbsolutePath().normalize();

        if (normalizedDirectory.equals(
                leftController.getCurrentDirectory()
        )) {
            leftController.refresh();
        }

        if (normalizedDirectory.equals(
                rightController.getCurrentDirectory()
        )) {
            rightController.refresh();
        }
    }

    private void updatePanelsAfterRename(
            Path oldPath,
            Path newPath
    ) {
        updatePanelAfterRename(
                leftController, oldPath, newPath
        );

        updatePanelAfterRename(
                rightController, oldPath, newPath
        );

        refreshAffectedPanels(oldPath.getParent());
    }

    private void updatePanelAfterRename(
            FilePanelController controller,
            Path oldPath,
            Path newPath
    ) {
        Path currentDirectory =
                controller.getCurrentDirectory();

        if (currentDirectory == null) {
            return;
        }

        if (currentDirectory.startsWith(oldPath)) {
            Path relativePath =
                    oldPath.relativize(currentDirectory);

            controller.navigateTo(
                    newPath.resolve(relativePath)
            );
        }
    }

    private void updatePanelsAfterDelete(
            Path sourceDirectory,
            List<Path> requestedPaths
    ) {
        updatePanelAfterDelete(
                leftController,
                sourceDirectory,
                requestedPaths
        );

        updatePanelAfterDelete(
                rightController,
                sourceDirectory,
                requestedPaths
        );
    }


    private void updatePanelAfterDelete(
            FilePanelController controller,
            Path sourceDirectory,
            List<Path> requestedPaths
    ) {
        Path currentDirectory =
                controller.getCurrentDirectory();

        if (currentDirectory == null) {
            return;
        }

        boolean affected = currentDirectory.equals(
                sourceDirectory
        ) || requestedPaths.stream()
                .anyMatch(currentDirectory::startsWith);

        if (!affected) {
            return;
        }

        if (Files.isDirectory(currentDirectory)
                && Files.isReadable(currentDirectory)) {
            controller.refresh();
            return;
        }

        Path availableDirectory =
                currentDirectory.getParent();

        while (availableDirectory != null) {
            if (Files.isDirectory(availableDirectory)
                    && Files.isReadable(availableDirectory)) {

                controller.navigateTo(availableDirectory);

                return;
            }

            availableDirectory =
                    availableDirectory.getParent();
        }

        showWarning(
                "Оновлення файлової панелі",
                "Не вдалося знайти доступний каталог."
        );
    }


    private Window getOwner() {
        return view.getScene() == null
                ? null
                : view.getScene().getWindow();
    }

    private void showWarning(
            String header,
            String message
    ) {
        showAlert(
                Alert.AlertType.WARNING,
                "Попередження",
                header,
                message
        );
    }

    private void showOperationError(
            String header,
            Exception exception
    ) {
        LOGGER.log(Level.WARNING, header, exception);

        String message;

        if (exception instanceof FileAlreadyExistsException) {
            message =
                    "Об'єкт із такою назвою вже існує.";

        } else if (exception instanceof AccessDeniedException
                || exception instanceof SecurityException) {

            message =
                    "Недостатньо прав доступу "
                            + "для виконання операції.";

        } else if (exception instanceof NoSuchFileException) {

            message =
                    "Файл або каталог не існує.";

        } else if (exception instanceof DirectoryNotEmptyException) {

            message =
                    "Не вдалося видалити каталог, "
                            + "оскільки він містить інші об'єкти.";

        } else if (exception instanceof InvalidPathException) {

            message =
                    "Вказано недопустиму назву або шлях.";

        } else if (exception.getClass() == IOException.class
                && exception.getMessage() != null) {

            message = exception.getMessage();

        } else {

            message =
                    "Під час виконання файлової операції "
                            + "виникла помилка.";
        }

        showAlert(
                Alert.AlertType.ERROR,
                "Помилка",
                header,
                message
        );
    }


    private void showAlert(
            Alert.AlertType type,
            String title,
            String header,
            String message
    ) {
        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        Window owner = getOwner();

        if (owner != null) {
            alert.initOwner(owner);
        }

        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/css/styles.css"),
                        "Не знайдено файл стилів styles.css."
                ).toExternalForm()
        );

        Button okButton = (Button) alert.getDialogPane()
                .lookupButton(ButtonType.OK);

        if (okButton != null) {
            okButton.setText("Гаразд");
        }

        alert.showAndWait();
    }


    private void setActivePanel(
            FilePanelController controller
    ) {
        activeController = controller;

        boolean leftActive =
                activeController == leftController;

        view.getLeftPanel().pseudoClassStateChanged(
                ACTIVE_PANEL,
                leftActive
        );

        view.getRightPanel().pseudoClassStateChanged(
                ACTIVE_PANEL,
                !leftActive
        );

        view.setStatus(
                leftActive
                        ? "Активна панель: ліва"
                        : "Активна панель: права"
        );
    }

    private void switchActivePanel() {
        if (activeController == leftController) {

            setActivePanel(rightController);

            view.getRightPanel()
                    .getFileTable()
                    .requestFocus();

        } else {

            setActivePanel(leftController);

            view.getLeftPanel()
                    .getFileTable()
                    .requestFocus();
        }
    }

    public FilePanelController getActiveController() {
        return activeController;
    }

    public FilePanelController getPassiveController() {
        return activeController == leftController
                ? rightController
                : leftController;
    }
}
