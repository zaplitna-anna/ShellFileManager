package com.shell.controller;

import com.shell.observer.FileSystemEventPublisher;
import com.shell.service.FileOperationService;
import com.shell.service.NavigationService;
import com.shell.view.FilePanelView;
import com.shell.view.MainView;
import com.shell.view.dialog.AlertDialogs;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import java.util.Objects;

public class MainController {
    private static final PseudoClass ACTIVE_PANEL =
            PseudoClass.getPseudoClass("active");

    private final MainView view;
    private final FilePanelController leftController;
    private final FilePanelController rightController;
    private final BackgroundOperationExecutor executor;
    private final FileOperationController fileOperationController;
    private FilePanelController activeController;

    private final ChangeListener<Node> focusListener =
            (observable, oldNode, newNode) ->
                    updateActivePanelByFocus(newNode);

    public MainController(
            MainView view,
            NavigationService navigationService,
            FileOperationService fileOperationService,
            FileSystemEventPublisher eventPublisher
    ) {
        this.view = Objects.requireNonNull(
                view, "Головне представлення не може бути null."
        );
        Objects.requireNonNull(navigationService,
                "Сервіс навігації не може бути null.");
        Objects.requireNonNull(fileOperationService,
                "Сервіс файлових операцій не може бути null.");
        Objects.requireNonNull(eventPublisher,
                "Видавець подій не може бути null.");

        leftController = new FilePanelController(
                view.getLeftPanel(), navigationService
        );
        rightController = new FilePanelController(
                view.getRightPanel(), navigationService
        );
        activeController = leftController;

        AlertDialogs alerts = new AlertDialogs(this::getOwner);
        executor = new BackgroundOperationExecutor(
                view, this::getOwner, alerts
        );
        fileOperationController = new FileOperationController(
                view, navigationService, fileOperationService,
                executor, alerts,
                this::getActiveController,
                this::getPassiveController,
                this::getOwner
        );

        leftController.setOnFileOpen(fileOperationController::handleOpen);
        rightController.setOnFileOpen(fileOperationController::handleOpen);
        eventPublisher.addObserver(leftController);
        eventPublisher.addObserver(rightController);

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
            if (!isOperationRunning()) {
                Platform.exit();
            }
        });
    }

    private void configureOperationEvents() {
        view.getCreateButton().setOnAction(
                event -> fileOperationController.handleCreate()
        );

        view.getCreateMenuItem().setOnAction(
                event -> fileOperationController.handleCreate()
        );

        view.getRenameButton().setOnAction(
                event -> fileOperationController.handleRename()
        );

        view.getRenameMenuItem().setOnAction(
                event -> fileOperationController.handleRename()
        );

        view.getCopyButton().setOnAction(
                event -> fileOperationController.handleCopy()
        );

        view.getCopyMenuItem().setOnAction(
                event -> fileOperationController.handleCopy()
        );

        view.getMoveButton().setOnAction(
                event -> fileOperationController.handleMove()
        );

        view.getMoveMenuItem().setOnAction(
                event -> fileOperationController.handleMove()
        );

        view.getDeleteButton().setOnAction(
                event -> fileOperationController.handleDelete()
        );

        view.getDeleteMenuItem().setOnAction(
                event -> fileOperationController.handleDelete()
        );
    }

    private void configureKeyboardEvents() {
        view.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (isOperationRunning()) {
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
                fileOperationController.handleRename();
            }
        });
    }

    private Window getOwner() {
        return view.getScene() == null
                ? null
                : view.getScene().getWindow();
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

        if (!isOperationRunning()) {
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
        return executor.isOperationRunning();
    }
}
