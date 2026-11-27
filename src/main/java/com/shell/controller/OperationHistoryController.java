package com.shell.controller;

import com.shell.model.OperationHistoryEntry;
import com.shell.service.FileOperationService;
import com.shell.view.dialog.AlertDialogs;
import com.shell.view.dialog.OperationHistoryDialog;

import javafx.scene.control.Alert;
import javafx.stage.Window;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OperationHistoryController {

    private static final Logger LOGGER =
            Logger.getLogger(
                    OperationHistoryController.class.getName()
            );

    private final FileOperationService fileOperationService;
    private final Supplier<Window> ownerSupplier;
    private final AlertDialogs alerts;

    public OperationHistoryController(
            FileOperationService fileOperationService,
            Supplier<Window> ownerSupplier,
            AlertDialogs alerts
    ) {

        this.fileOperationService =
                Objects.requireNonNull(
                        fileOperationService,
                        "Сервіс файлових операцій "
                                + "не може бути null."
                );

        this.ownerSupplier =
                Objects.requireNonNull(
                        ownerSupplier,
                        "Постачальник вікна не може бути null."
                );

        this.alerts =
                Objects.requireNonNull(
                        alerts,
                        "Компонент повідомлень "
                                + "не може бути null."
                );
    }

    public void handleOperationHistory() {

        try {
            List<OperationHistoryEntry> history =
                    fileOperationService.getOperationHistory();

            OperationHistoryDialog dialog =
                    new OperationHistoryDialog(
                            ownerSupplier.get(),
                            history
                    );

            dialog.showAndWait();

        } catch (SQLException exception) {

            LOGGER.log(
                    Level.WARNING,
                    "Не вдалося отримати "
                            + "журнал файлових операцій.",
                    exception
            );

            alerts.showAlert(
                    Alert.AlertType.ERROR,
                    "Помилка",
                    "Журнал файлових операцій",
                    "Не вдалося завантажити "
                            + "журнал файлових операцій."
            );
        }
    }
}
