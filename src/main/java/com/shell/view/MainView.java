
package com.shell.view;

import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCodeCombination;
import javafx.geometry.Insets;

public class MainView extends BorderPane {
    private final FilePanelView leftPanel = new FilePanelView();
    private final FilePanelView rightPanel = new FilePanelView();

    private final Button createButton = new Button("Створити (F7)");
    private final Button renameButton = new Button("Перейменувати (F2)");
    private final Button copyButton = new Button("Копіювати (F5)");
    private final Button moveButton = new Button("Перемістити (F6)");
    private final Button deleteButton = new Button("Видалити (F8)");
    private final Button searchButton = new Button("Пошук (Alt+F7)");

    private final MenuItem exitMenuItem =
            new MenuItem("Вихід");

    private final MenuItem createMenuItem =
            new MenuItem("Створити");

    private final MenuItem renameMenuItem =
            new MenuItem("Перейменувати");

    private final MenuItem copyMenuItem =
            new MenuItem("Копіювати");

    private final MenuItem moveMenuItem =
            new MenuItem("Перемістити");

    private final MenuItem deleteMenuItem =
            new MenuItem("Видалити");

    private final MenuItem searchMenuItem =
            new MenuItem("Пошук файлів і каталогів");

    private final MenuItem operationHistoryMenuItem =
            new MenuItem("Журнал файлових операцій");

    private final MenuItem searchHistoryMenuItem =
            new MenuItem("Історія пошуку");

    private final Label statusLabel =
            new Label("Готово до роботи");

    public MainView() {
        getStyleClass().add("main-view");

        configureTop();
        configureCenter();
        configureBottom();
        configureShortcuts();
    }

    private void configureTop() {
        MenuBar menuBar = createMenuBar();
        ToolBar toolBar = createToolBar();

        setTop(new VBox(menuBar, toolBar));
    }

    private MenuBar createMenuBar() {
        Menu fileMenu = new Menu("Файл");

        fileMenu.getItems().add(exitMenuItem);

        Menu operationsMenu = new Menu("Операції");

        operationsMenu.getItems().addAll(
                createMenuItem,
                renameMenuItem,
                new SeparatorMenuItem(),
                copyMenuItem,
                moveMenuItem,
                new SeparatorMenuItem(),
                deleteMenuItem
        );

        Menu searchMenu = new Menu("Пошук");

        searchMenu.getItems().add(searchMenuItem);

        Menu historyMenu = new Menu("Історія");

        historyMenu.getItems().addAll(
                operationHistoryMenuItem,
                searchHistoryMenuItem
        );

        return new MenuBar(
                fileMenu,
                operationsMenu,
                searchMenu,
                historyMenu
        );
    }

    private ToolBar createToolBar() {
        createButton.setTooltip(
                new Tooltip("Створити файл або каталог")
        );

        renameButton.setTooltip(
                new Tooltip("Перейменувати вибраний об'єкт")
        );

        copyButton.setTooltip(
                new Tooltip("Копіювати до пасивної панелі")
        );

        moveButton.setTooltip(
                new Tooltip("Перемістити до пасивної панелі")
        );

        deleteButton.setTooltip(
                new Tooltip("Видалити вибрані об'єкти")
        );

        searchButton.setTooltip(
                new Tooltip("Пошук файлів і каталогів")
        );

        return new ToolBar(
                createButton,
                renameButton,
                new Separator(),
                copyButton,
                moveButton,
                new Separator(),
                deleteButton,
                new Separator(),
                searchButton
        );
    }

    private void configureShortcuts() {
        createMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.F7)
        );

        renameMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.F2)
        );

        copyMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.F5)
        );

        moveMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.F6)
        );

        deleteMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.F8)
        );

        searchMenuItem.setAccelerator(
                new KeyCodeCombination(
                        KeyCode.F7,
                        KeyCombination.ALT_DOWN
                )
        );
    }

    private void configureCenter() {
        SplitPane splitPane = new SplitPane(
                leftPanel,
                rightPanel
        );

        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.5);

        leftPanel.setMinWidth(250);
        rightPanel.setMinWidth(250);

        setCenter(splitPane);
    }

    private void configureBottom() {
        statusLabel.getStyleClass().add("main-status");

        statusLabel.setPadding(new Insets(6, 10, 6, 10));
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        setBottom(statusLabel);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    public FilePanelView getLeftPanel() {
        return leftPanel;
    }

    public FilePanelView getRightPanel() {
        return rightPanel;
    }

    public Button getCreateButton() {
        return createButton;
    }

    public Button getRenameButton() {
        return renameButton;
    }

    public Button getCopyButton() {
        return copyButton;
    }

    public Button getMoveButton() {
        return moveButton;
    }

    public Button getDeleteButton() {
        return deleteButton;
    }

    public Button getSearchButton() {
        return searchButton;
    }

    public MenuItem getExitMenuItem() {
        return exitMenuItem;
    }

    public MenuItem getCreateMenuItem() {
        return createMenuItem;
    }

    public MenuItem getRenameMenuItem() {
        return renameMenuItem;
    }

    public MenuItem getCopyMenuItem() {
        return copyMenuItem;
    }

    public MenuItem getMoveMenuItem() {
        return moveMenuItem;
    }

    public MenuItem getDeleteMenuItem() {
        return deleteMenuItem;
    }

    public MenuItem getSearchMenuItem() {
        return searchMenuItem;
    }

    public MenuItem getOperationHistoryMenuItem() {
        return operationHistoryMenuItem;
    }

    public MenuItem getSearchHistoryMenuItem() {
        return searchHistoryMenuItem;
    }
}