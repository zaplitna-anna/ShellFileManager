package com.shell.view.dialog;

import com.shell.util.OperationErrorFormatter;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Window;

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

        showAlert(
                Alert.AlertType.ERROR,
                "Помилка",
                header,
                OperationErrorFormatter.format(exception)
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
