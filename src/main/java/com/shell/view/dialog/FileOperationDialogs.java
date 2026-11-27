package com.shell.view.dialog;

import com.shell.model.FileSystemItem;
import com.shell.model.FileSystemItemType;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.scene.layout.Region;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.List;

public final class FileOperationDialogs {

    private FileOperationDialogs() {
    }

    public record CreateResult(
            String name,
            FileSystemItemType type
    ) {
    }

    public static Optional<CreateResult> showCreateDialog(
            Window owner
    ) {
        Dialog<CreateResult> dialog = new Dialog<>();

        dialog.setTitle("Створення об'єкта");
        dialog.setHeaderText("Створення файлу або каталогу");

        ButtonType createButtonType = new ButtonType(
                "Створити",
                ButtonBar.ButtonData.OK_DONE
        );

        ButtonType cancelButtonType = new ButtonType(
                "Скасувати",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                createButtonType,
                cancelButtonType
        );

        ToggleGroup typeGroup = new ToggleGroup();

        RadioButton fileRadio = new RadioButton("Файл");
        fileRadio.setToggleGroup(typeGroup);
        fileRadio.setSelected(true);

        RadioButton directoryRadio =
                new RadioButton("Каталог");

        directoryRadio.setToggleGroup(typeGroup);

        TextField nameField = new TextField();
        nameField.setPromptText("Введіть назву об'єкта");

        HBox typeBox = new HBox(
                15,
                fileRadio,
                directoryRadio
        );

        VBox content = new VBox(
                12,
                new Label("Тип об'єкта:"),
                typeBox,
                new Label("Назва:"),
                nameField
        );

        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);

        Node createButton = dialog.getDialogPane()
                .lookupButton(createButtonType);

        createButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> nameField.getText().isBlank(),
                        nameField.textProperty()
                )
        );

        dialog.setResultConverter(buttonType -> {
            if (buttonType == createButtonType) {
                FileSystemItemType type =
                        fileRadio.isSelected()
                                ? FileSystemItemType.FILE
                                : FileSystemItemType.DIRECTORY;

                return new CreateResult(
                        nameField.getText(),
                        type
                );
            }

            return null;
        });

        configureDialog(dialog, owner);

        dialog.setOnShown(event ->
                Platform.runLater(nameField::requestFocus)
        );

        return dialog.showAndWait();
    }

    public static Optional<String> showRenameDialog(
            Window owner,
            FileSystemItem item
    ) {
        TextInputDialog dialog =
                new TextInputDialog(item.getName());

        dialog.setTitle("Перейменування");
        dialog.setHeaderText(
                "Поточна назва: " + item.getName()
        );

        dialog.setContentText("Нова назва:");

        Button saveButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);

        saveButton.setText("Зберегти");

        Button cancelButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.CANCEL);

        cancelButton.setText("Скасувати");

        saveButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> dialog.getEditor().getText().isBlank(),
                        dialog.getEditor().textProperty()
                )
        );

        configureDialog(dialog, owner);

        dialog.setOnShown(event ->
                Platform.runLater(() -> {
                    dialog.getEditor().requestFocus();
                    dialog.getEditor().selectAll();
                })
        );

        return dialog.showAndWait();
    }


    public static boolean showDeleteConfirmation(
            Window owner,
            List<FileSystemItem> items
    ) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        alert.setTitle("Підтвердження видалення");
        alert.setHeaderText("Видалити вибрані об'єкти?");

        ButtonType deleteButtonType = new ButtonType(
                "Видалити",
                ButtonBar.ButtonData.OK_DONE
        );

        ButtonType cancelButtonType = new ButtonType(
                "Скасувати",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        alert.getButtonTypes().setAll(
                deleteButtonType,
                cancelButtonType
        );

        ListView<String> itemList = new ListView<>();

        itemList.getItems().setAll(
                items.stream()
                        .map(FileSystemItem::getName)
                        .toList()
        );

        itemList.setPrefHeight(
                Math.min(150, items.size() * 30 + 5)
        );

        itemList.setMinHeight(Region.USE_PREF_SIZE);

        Label countLabel = new Label(
                "Вибрано об'єктів: " + items.size()
        );

        Label warningLabel = new Label(
                "Об'єкти та весь вміст каталогів буде "
                        + "видалено без переміщення до кошика."
        );

        warningLabel.setWrapText(true);

        warningLabel.setMaxWidth(Double.MAX_VALUE);
        warningLabel.setMinHeight(Region.USE_PREF_SIZE);

        VBox content = new VBox(
                12,
                countLabel,
                itemList,
                warningLabel
        );

        content.setPadding(new Insets(10));

        alert.getDialogPane().setContent(content);

        configureDialog(alert, owner);

        Button deleteButton = (Button) alert.getDialogPane()
                .lookupButton(deleteButtonType);

        Button cancelButton = (Button) alert.getDialogPane()
                .lookupButton(cancelButtonType);

        deleteButton.setDefaultButton(false);
        cancelButton.setDefaultButton(true);

        alert.setOnShown(event ->
                Platform.runLater(cancelButton::requestFocus)
        );

        return alert.showAndWait()
                .filter(result -> result == deleteButtonType)
                .isPresent();
    }


    public static boolean showCopyConfirmation(
            Window owner,
            List<FileSystemItem> items,
            Path destinationDirectory
    ) {
        if (items == null || items.isEmpty()
                || destinationDirectory == null) {
            return false;
        }

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        alert.setTitle("Підтвердження копіювання");
        alert.setHeaderText("Копіювати вибрані об'єкти?");

        ButtonType copyButtonType = new ButtonType(
                "Копіювати",
                ButtonBar.ButtonData.OK_DONE
        );

        ButtonType cancelButtonType = new ButtonType(
                "Скасувати",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        alert.getButtonTypes().setAll(
                copyButtonType,
                cancelButtonType
        );

        Label countLabel = new Label(
                "Вибрано об'єктів: " + items.size()
        );

        ListView<String> itemList = new ListView<>();

        itemList.getItems().setAll(
                items.stream()
                        .map(FileSystemItem::getName)
                        .toList()
        );

        itemList.setPrefHeight(
                Math.min(150, items.size() * 30 + 5)
        );

        Label destinationLabel = new Label(
                "Каталог призначення:"
        );

        Label pathLabel = new Label(
                destinationDirectory.toString()
        );

        pathLabel.setWrapText(true);
        pathLabel.setMaxWidth(Double.MAX_VALUE);
        pathLabel.setMinHeight(Region.USE_PREF_SIZE);

        VBox content = new VBox(
                12,
                countLabel,
                itemList,
                destinationLabel,
                pathLabel
        );

        content.setPadding(new Insets(10));

        alert.getDialogPane().setContent(content);

        configureDialog(alert, owner);

        return alert.showAndWait()
                .filter(result -> result == copyButtonType)
                .isPresent();
    }


    public static boolean showMoveConfirmation(
            Window owner,
            List<FileSystemItem> items,
            Path destinationDirectory
    ) {
        if (items == null || items.isEmpty()
                || destinationDirectory == null) {
            return false;
        }

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        alert.setTitle("Підтвердження переміщення");
        alert.setHeaderText("Перемістити вибрані об'єкти?");

        ButtonType moveButtonType = new ButtonType(
                "Перемістити",
                ButtonBar.ButtonData.OK_DONE
        );

        ButtonType cancelButtonType = new ButtonType(
                "Скасувати",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        alert.getButtonTypes().setAll(
                moveButtonType,
                cancelButtonType
        );

        Label countLabel = new Label(
                "Вибрано об'єктів: " + items.size()
        );

        ListView<String> itemList = new ListView<>();

        itemList.getItems().setAll(
                items.stream()
                        .map(FileSystemItem::getName)
                        .toList()
        );

        itemList.setPrefHeight(
                Math.min(150, items.size() * 30 + 5)
        );

        Label destinationLabel = new Label(
                "Каталог призначення:"
        );

        Label pathLabel = new Label(
                destinationDirectory.toString()
        );

        pathLabel.setWrapText(true);
        pathLabel.setMaxWidth(Double.MAX_VALUE);
        pathLabel.setMinHeight(Region.USE_PREF_SIZE);

        Label warningLabel = new Label(
                "Після успішного переміщення об'єкти "
                        + "буде видалено з початкового каталогу."
        );

        warningLabel.setWrapText(true);
        warningLabel.setMaxWidth(Double.MAX_VALUE);
        warningLabel.setMinHeight(Region.USE_PREF_SIZE);

        VBox content = new VBox(
                12,
                countLabel,
                itemList,
                destinationLabel,
                pathLabel,
                warningLabel
        );

        content.setPadding(new Insets(10));

        alert.getDialogPane().setContent(content);

        configureDialog(alert, owner);

        return alert.showAndWait()
                .filter(result -> result == moveButtonType)
                .isPresent();
    }


    private static void configureDialog(
            Dialog<?> dialog,
            Window owner
    ) {
        if (owner != null) {
            dialog.initOwner(owner);
        }

        dialog.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(
                        FileOperationDialogs.class.getResource(
                                "/css/styles.css"
                        ),
                        "Не знайдено файл стилів styles.css."
                ).toExternalForm()
        );

        dialog.getDialogPane().getStyleClass()
                .add("file-operation-dialog");

        dialog.getDialogPane().setPrefWidth(420);
    }
}