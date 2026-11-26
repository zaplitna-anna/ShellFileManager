package com.shell;

import com.shell.controller.MainController;
import com.shell.facade.FileSystemFacade;
import com.shell.service.NavigationService;
import com.shell.view.MainView;

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

        MainView mainView = new MainView();

        MainController mainController =
                new MainController(mainView, navigationService);

        Scene scene = new Scene(mainView, 1200, 750);

        stage.setTitle("Shell — файловий менеджер");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);

        stage.show();

        mainController.initialize();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
