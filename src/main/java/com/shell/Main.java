package com.shell;

import com.shell.command.CommandInvoker;
import com.shell.controller.MainController;
import com.shell.facade.FileSystemFacade;
import com.shell.observer.FileSystemEventPublisher;
import com.shell.service.FileOperationService;
import com.shell.service.NavigationService;
import com.shell.view.MainView;

import java.util.Objects;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.shell.service.SearchService;
import com.shell.database.DatabaseManager;
import com.shell.view.dialog.AlertDialogs;

import javafx.scene.control.Alert;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.shell.database.SearchHistoryRepository;

public class Main extends Application {
    private static final Logger LOGGER =
            Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage stage) {
        DatabaseManager databaseManager =
                new DatabaseManager();

        try {
            databaseManager.initialize();

            LOGGER.log(
                    Level.INFO,
                    "Базу даних успішно ініціалізовано: {0}",
                    databaseManager.getDatabasePath()
            );

        } catch (IOException | SQLException | SecurityException exception) {

            LOGGER.log(
                    Level.SEVERE,
                    "Не вдалося ініціалізувати базу даних.",
                    exception
            );

            AlertDialogs alerts =
                    new AlertDialogs(() -> null);

            alerts.showAlert(
                    Alert.AlertType.ERROR,
                    "Помилка",
                    "Не вдалося ініціалізувати базу даних",
                    "Запуск програми неможливий. "
                            + "Перевірте доступність "
                            + "каталогу бази даних."
            );

            return;
        }


        FileSystemFacade fileSystemFacade =
                new FileSystemFacade();

        NavigationService navigationService =
                new NavigationService(fileSystemFacade);

        CommandInvoker commandInvoker =
                new CommandInvoker();

        FileSystemEventPublisher eventPublisher =
                new FileSystemEventPublisher();

        FileOperationService fileOperationService =
                new FileOperationService(
                        fileSystemFacade,
                        commandInvoker,
                        eventPublisher
                );

        SearchHistoryRepository searchHistoryRepository =
                new SearchHistoryRepository(
                        databaseManager
                );

        SearchService searchService =
                new SearchService(
                        fileSystemFacade,
                        searchHistoryRepository
                );

        MainView mainView = new MainView();

        MainController mainController =
                new MainController(
                        mainView,
                        navigationService,
                        fileOperationService,
                        eventPublisher,
                        searchService
                );


        Scene scene = new Scene(mainView, 1200, 750);

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/css/styles.css"),
                        "Не знайдено файл стилів styles.css."
                ).toExternalForm()
        );

        stage.setTitle("Shell — файловий менеджер");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            if (mainController.isOperationRunning()) {
                event.consume();
            }
        });

        stage.show();

        mainController.initialize();
    }

    public static void main(String[] args) {
        launch(args);
    }
}