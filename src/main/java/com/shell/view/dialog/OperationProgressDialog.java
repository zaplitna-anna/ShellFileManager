package com.shell.view.dialog;

import com.shell.model.OperationProgress;
import com.shell.observer.OperationProgressObserver;
import com.shell.operation.CancellationToken;

import java.util.Locale;
import java.util.Objects;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class OperationProgressDialog
        implements OperationProgressObserver {

    private final Stage stage;

    private final CancellationToken cancellationToken;

    private final Label statusLabel;
    private final ProgressBar progressBar;
    private final Label percentLabel;
    private final Label currentPathLabel;
    private final Label itemsLabel;
    private final Label bytesLabel;
    private final Button cancelButton;

    private boolean cancellationRequested;
    private boolean closed;

    public OperationProgressDialog(
            Window owner,
            String operationTitle,
            CancellationToken cancellationToken
    ) {
        this.cancellationToken = Objects.requireNonNull(
                cancellationToken,
                "Механізм скасування не може бути null."
        );

        stage = new Stage();

        if (owner != null) {
            stage.initOwner(owner);
        }

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(operationTitle);
        stage.setResizable(false);

        statusLabel = new Label(
                "Підрахунок обсягу роботи..."
        );

        progressBar = new ProgressBar(-1.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        percentLabel = new Label("—");
        percentLabel.setMinWidth(50);
        percentLabel.setAlignment(Pos.CENTER_RIGHT);

        currentPathLabel = new Label(
                "Поточний об'єкт: —"
        );

        currentPathLabel.setWrapText(true);
        currentPathLabel.setMaxWidth(Double.MAX_VALUE);
        currentPathLabel.setMinHeight(
                Region.USE_PREF_SIZE
        );

        itemsLabel = new Label(
                "Оброблено об'єктів: 0"
        );

        bytesLabel = new Label(
                "Оброблено даних: 0 Б"
        );

        cancelButton = new Button("Скасувати");

        cancelButton.setOnAction(
                event -> requestCancellation()
        );

        stage.setOnCloseRequest(event -> {
            event.consume();
            requestCancellation();
        });

        HBox buttonBox = new HBox(cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(
                12,
                statusLabel,
                progressBar,
                percentLabel,
                currentPathLabel,
                itemsLabel,
                bytesLabel,
                buttonBox
        );

        root.setPadding(new Insets(20));
        root.setPrefWidth(480);

        Scene scene = new Scene(root);

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource(
                                "/css/styles.css"
                        ),
                        "Не знайдено файл стилів styles.css."
                ).toExternalForm()
        );

        stage.setScene(scene);
        stage.sizeToScene();
    }

    public void show() {
        stage.show();
    }

    public void close() {
        closed = true;
        stage.hide();
    }

    private void requestCancellation() {
        if (cancellationRequested || closed) {
            return;
        }

        cancellationRequested = true;

        cancellationToken.cancel();

        cancelButton.setDisable(true);

        statusLabel.setText(
                "Скасування операції..."
        );

        progressBar.setProgress(-1.0);
        percentLabel.setText("—");
    }

    @Override
    public void onProgressChanged(
            OperationProgress progress
    ) {
        if (Platform.isFxApplicationThread()) {
            updateProgress(progress);
        } else {
            Platform.runLater(
                    () -> updateProgress(progress)
            );
        }
    }

    private void updateProgress(
            OperationProgress progress
    ) {
        if (closed) {
            return;
        }

        if (!cancellationRequested) {

            double value = progress.getProgress();

            progressBar.setProgress(value);

            if (value < 0) {

                statusLabel.setText(
                        "Підрахунок обсягу роботи..."
                );

                percentLabel.setText("—");

            } else {

                statusLabel.setText(
                        "Виконання файлової операції..."
                );

                percentLabel.setText(
                        Math.min(
                                99,
                                (int) (value * 100)
                        ) + "%"
                );
            }
        }

        currentPathLabel.setText(
                "Поточний об'єкт: "
                        + (progress.currentPath() == null
                        ? "—"
                        : progress.currentPath())
        );

        if (progress.totalItems() > 0) {
            itemsLabel.setText(
                    "Виконано етапів: "
                            + progress.processedItems()
                            + " із "
                            + progress.totalItems()
            );
        } else {
            itemsLabel.setText(
                    "Виконано етапів: "
                            + progress.processedItems()
            );
        }

        if (progress.totalBytes() > 0) {
            bytesLabel.setText(
                    "Оброблено даних: "
                            + formatBytes(
                            progress.processedBytes()
                    )
                            + " із "
                            + formatBytes(
                            progress.totalBytes()
                    )
            );
        } else {
            bytesLabel.setText(
                    "Оброблено даних: "
                            + formatBytes(
                            progress.processedBytes()
                    )
            );
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " Б";
        }

        if (bytes < 1024L * 1024) {
            return String.format(
                    Locale.forLanguageTag("uk-UA"),
                    "%.1f КіБ",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024 * 1024) {
            return String.format(
                    Locale.forLanguageTag("uk-UA"),
                    "%.1f МіБ",
                    bytes / (1024.0 * 1024)
            );
        }

        return String.format(
                Locale.forLanguageTag("uk-UA"),
                "%.1f ГіБ",
                bytes / (1024.0 * 1024 * 1024)
        );
    }
}