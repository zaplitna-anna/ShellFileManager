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

public class Main extends Application {

    @Override
    public void start(Stage stage) {

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

        MainView mainView = new MainView();

        MainController mainController =
                new MainController(
                        mainView,
                        navigationService,
                        fileOperationService,
                        eventPublisher
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