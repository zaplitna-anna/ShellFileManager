package com.shell.controller;

import com.shell.model.FileSystemItem;
import com.shell.model.FileSystemItemType;
import com.shell.observer.FileSystemEventPublisher;
import com.shell.observer.OperationProgressPublisher;
import com.shell.operation.CancellationToken;
import com.shell.operation.OperationContext;
import com.shell.service.FileOperationService;
import com.shell.service.NavigationService;
import com.shell.view.FilePanelView;
import com.shell.view.MainView;
import com.shell.view.dialog.FileOperationDialogs;
import com.shell.view.dialog.OperationProgressDialog;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Window;


public class MainController {
    private static final Logger LOGGER =
            Logger.getLogger(MainController.class.getName());

    private static final PseudoClass ACTIVE_PANEL =
            PseudoClass.getPseudoClass("active");

    private final MainView view;
    private final NavigationService navigationService;
    private final FileOperationService fileOperationService;

    private final FilePanelController leftController;
    private final FilePanelController rightController;

    private FilePanelController activeController;

    private boolean operationRunning;

    private final ChangeListener<Node> focusListener =
            (observable, oldNode, newNode) ->
                    updateActivePanelByFocus(newNode);

    @FunctionalInterface
    private interface FileOperation {

        void execute(OperationContext context)
                throws IOException;
    }

    public MainController(
            MainView view,
            NavigationService navigationService,
            FileOperationService fileOperationService,
            FileSystemEventPublisher eventPublisher
    ) {
        this.view = Objects.requireNonNull(
                view,
                "Головне представлення не може бути null."
        );

        this.navigationService = Objects.requireNonNull(
                navigationService,
                "Сервіс навігації не може бути null."
        );

        this.fileOperationService = Objects.requireNonNull(
                fileOperationService,
                "Сервіс файлових операцій не може бути null."
        );

        Objects.requireNonNull(
                eventPublisher,
                "Видавець подій не може бути null."
        );

        leftController = new FilePanelController(
                view.getLeftPanel(),
                this.navigationService
        );

        rightController = new FilePanelController(
                view.getRightPanel(),
                this.navigationService
        );

        leftController.setOnFileOpen(this::handleOpen);
        rightController.setOnFileOpen(this::handleOpen);

        eventPublisher.addObserver(leftController);
        eventPublisher.addObserver(rightController);

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
        view.getExitMenuItem().setOnAction(event -> {
            if (!operationRunning) {
                Platform.exit();
            }
        });
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

        view.getCopyButton().setOnAction(
                event -> handleCopy()
        );

        view.getCopyMenuItem().setOnAction(
                event -> handleCopy()
        );

        view.getMoveButton().setOnAction(
                event -> handleMove()
        );

        view.getMoveMenuItem().setOnAction(
                event -> handleMove()
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
            if (operationRunning) {
                event.consume();
                return;
            }

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
        if (operationRunning) {
            return;
        }
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
                        view.setStatus(
                                "Не вдалося створити об'єкт: "
                                        + result.name()
                        );
                    }
                });
    }


    private void handleRename() {
        if (operationRunning) {
            return;
        }

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
                view.setStatus(
                        "Не вдалося перейменувати об'єкт: "
                                + item.getName()
                );
            }
        });
    }


    private void handleCopy() {
        if (operationRunning) {
            return;
        }
        FilePanelController sourceController =
                getActiveController();

        FilePanelController destinationController =
                getPassiveController();

        List<FileSystemItem> selectedItems =
                sourceController.getSelectedItems();

        if (selectedItems.isEmpty()) {
            showWarning(
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

            showWarning(
                    "Копіювання",
                    "Не вдалося визначити каталог "
                            + "джерела або призначення."
            );

            return;
        }

        try {
            if (navigationService.isSameDirectory(
                    sourceDirectory,
                    destinationDirectory
            )) {
                showWarning(
                        "Копіювання",
                        "Каталоги джерела та призначення "
                                + "збігаються."
                );

                return;
            }

        } catch (IOException | SecurityException e) {
            showOperationError(
                    "Не вдалося перевірити каталоги",
                    e
            );

            return;
        }

        List<Path> sourcePaths = selectedItems.stream()
                .map(FileSystemItem::getPath)
                .toList();

        boolean confirmed =
                FileOperationDialogs.showCopyConfirmation(
                        getOwner(),
                        selectedItems,
                        destinationDirectory
                );


        if (!confirmed) {
            return;
        }


        executeInBackground(
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


    private void handleMove() {
        if (operationRunning) {
            return;
        }

        FilePanelController sourceController =
                getActiveController();

        FilePanelController destinationController =
                getPassiveController();

        List<FileSystemItem> selectedItems =
                sourceController.getSelectedItems();

        if (selectedItems.isEmpty()) {
            showWarning(
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

            showWarning(
                    "Переміщення",
                    "Не вдалося визначити каталог "
                            + "джерела або призначення."
            );

            return;
        }

        try {
            if (navigationService.isSameDirectory(
                    sourceDirectory,
                    destinationDirectory
            )) {
                showWarning(
                        "Переміщення",
                        "Каталоги джерела та призначення "
                                + "збігаються."
                );

                return;
            }

        } catch (IOException | SecurityException e) {
            showOperationError(
                    "Не вдалося перевірити каталоги",
                    e
            );

            return;
        }

        List<Path> sourcePaths = selectedItems.stream()
                .map(FileSystemItem::getPath)
                .toList();

        boolean confirmed =
                FileOperationDialogs.showMoveConfirmation(
                        getOwner(),
                        selectedItems,
                        destinationDirectory
                );

        if (!confirmed) {
            return;
        }


        executeInBackground(
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


    private void handleDelete() {
        if (operationRunning) {
            return;
        }

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


        executeInBackground(
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


    private void handleOpen(FileSystemItem item) {
        if (operationRunning) {
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


    private void executeInBackground(
            FileOperation operation,
            String operationTitle,
            String runningMessage,
            String successMessage,
            String errorHeader,
            String failureMessage
    ) {
        if (operationRunning) {
            return;
        }

        CancellationToken cancellationToken =
                new CancellationToken();

        OperationProgressPublisher progressPublisher =
                new OperationProgressPublisher();

        OperationContext context =
                new OperationContext(
                        cancellationToken,
                        progressPublisher
                );

        OperationProgressDialog progressDialog =
                new OperationProgressDialog(
                        getOwner(),
                        operationTitle,
                        cancellationToken
                );

        progressPublisher.addObserver(progressDialog);

        Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws IOException {
                operation.execute(context);
                return null;
            }
        };

        task.setOnSucceeded(event -> {

            progressPublisher.removeObserver(
                    progressDialog
            );

            progressDialog.close();

            setOperationRunning(false);

            view.setStatus(successMessage);
        });

        task.setOnFailed(event -> {

            progressPublisher.removeObserver(
                    progressDialog
            );

            progressDialog.close();

            setOperationRunning(false);

            Throwable error = task.getException();

            if (error instanceof CancellationException) {

                view.setStatus(
                        "Операцію скасовано. "
                                + "Виконані зміни збережено."
                );

                LOGGER.log(
                        Level.INFO,
                        "Файлову операцію скасовано."
                );

                if (error.getSuppressed().length > 0) {
                    showWarning(
                            "Операцію скасовано",
                            "Не вдалося повністю очистити "
                                    + "незавершену копію файлу."
                    );
                }

                return;
            }

            if (error instanceof Exception exception) {

                showOperationError(
                        errorHeader,
                        exception
                );

            } else {

                LOGGER.log(
                        Level.SEVERE,
                        errorHeader,
                        error
                );

                showAlert(
                        Alert.AlertType.ERROR,
                        "Помилка",
                        errorHeader,
                        "Виникла непередбачена помилка."
                );
            }

            view.setStatus(failureMessage);
        });

        setOperationRunning(true);
        view.setStatus(runningMessage);

        try {
            Thread thread = new Thread(
                    task,
                    "file-operation-thread"
            );

            thread.setDaemon(false);

            progressDialog.show();
            thread.start();

        } catch (RuntimeException exception) {

            progressPublisher.removeObserver(
                    progressDialog
            );

            progressDialog.close();

            setOperationRunning(false);

            showOperationError(
                    errorHeader,
                    exception
            );

            view.setStatus(failureMessage);
        }
    }

    private void setOperationRunning(boolean running) {
        operationRunning = running;
        view.setDisable(running);
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

        } else if (exception instanceof UnsupportedOperationException) {
            message =
                    "Ця операція не підтримується "
                            + "поточною системою.";

        } else if (exception instanceof InvalidPathException) {

            message =
                    "Вказано недопустиму назву або шлях.";
        } else if (exception instanceof IllegalArgumentException) {
            message =
                    "Вказано некоректні параметри "
                            + "файлової операції.";

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

        alert.getDialogPane().setMinHeight(
                Region.USE_PREF_SIZE
        );

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

        if (!operationRunning) {
            view.setStatus(
                    leftActive
                            ? "Активна панель: ліва"
                            : "Активна панель: права"
            );
        }
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

    public boolean isOperationRunning() {
        return operationRunning;
    }
}
