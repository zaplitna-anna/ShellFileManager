package com.shell.view.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AlertDialogs {
    private static final Logger LOGGER =
            Logger.getLogger(AlertDialogs.class.getName());

    private final Supplier<Window> ownerSupplier;

    public AlertDialogs(Supplier<Window> ownerSupplier) {
        this.ownerSupplier = Objects.requireNonNull(
                ownerSupplier,
                "Постачальник вікна-власника не може бути null."
        );
    }

    public void showWarning(
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

    public void showOperationError(
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

    public void showAlert(
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

        Window owner = ownerSupplier.get();

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
}
