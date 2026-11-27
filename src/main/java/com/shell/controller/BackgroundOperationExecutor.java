package com.shell.controller;

import com.shell.observer.OperationProgressPublisher;
import com.shell.operation.CancellationToken;
import com.shell.operation.OperationContext;
import com.shell.view.MainView;
import com.shell.view.dialog.AlertDialogs;
import com.shell.view.dialog.OperationProgressDialog;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackgroundOperationExecutor {
    private static final Logger LOGGER =
            Logger.getLogger(BackgroundOperationExecutor.class.getName());

    private final MainView view;
    private final Supplier<Window> ownerSupplier;
    private final AlertDialogs alerts;
    private boolean operationRunning;

    @FunctionalInterface
    public interface FileOperation {
        void execute(OperationContext context) throws IOException;
    }

    public BackgroundOperationExecutor(
            MainView view,
            Supplier<Window> ownerSupplier,
            AlertDialogs alerts
    ) {
        this.view = Objects.requireNonNull(view);
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier);
        this.alerts = Objects.requireNonNull(alerts);
    }

    public void executeInBackground(
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
                        ownerSupplier.get(),
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
            finishOperation(progressPublisher, progressDialog);

            view.setStatus(successMessage);
        });

        task.setOnFailed(event -> {

            finishOperation(progressPublisher, progressDialog);

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
                    LOGGER.log(
                            Level.WARNING,
                            "Додаткові помилки під час скасування операції.",
                            error
                    );

                    alerts.showWarning(
                            "Операцію скасовано",
                            "Під час завершення скасованої операції "
                                    + "виникли додаткові помилки."
                    );
                }

                return;
            }

            if (error instanceof Exception exception) {

                alerts.showOperationError(
                        errorHeader,
                        exception
                );

            } else {

                LOGGER.log(
                        Level.SEVERE,
                        errorHeader,
                        error
                );

                alerts.showAlert(
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

            finishOperation(progressPublisher, progressDialog);

            alerts.showOperationError(
                    errorHeader,
                    exception
            );

            view.setStatus(failureMessage);
        }
    }

    private void finishOperation(
            OperationProgressPublisher progressPublisher,
            OperationProgressDialog progressDialog
    ) {
        progressPublisher.removeObserver(progressDialog);

        try {
            progressDialog.close();
        } finally {
            setOperationRunning(false);
        }
    }

    private void setOperationRunning(boolean running) {
        operationRunning = running;
        view.setDisable(running);
    }

    public boolean isOperationRunning() {
        return operationRunning;
    }
}
