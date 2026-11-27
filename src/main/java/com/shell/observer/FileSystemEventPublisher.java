package com.shell.observer;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileSystemEventPublisher {

    private static final Logger LOGGER =
            Logger.getLogger(
                    FileSystemEventPublisher.class.getName()
            );

    private final CopyOnWriteArrayList<FileSystemObserver> observers =
            new CopyOnWriteArrayList<>();

    public void addObserver(FileSystemObserver observer) {
        Objects.requireNonNull(
                observer,
                "Спостерігач не може бути null."
        );

        observers.addIfAbsent(observer);
    }

    public void removeObserver(FileSystemObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(FileSystemEvent event) {
        Objects.requireNonNull(
                event,
                "Подія файлової системи не може бути null."
        );

        for (FileSystemObserver observer : observers) {
            try {
                observer.onFileSystemChanged(event);

            } catch (RuntimeException exception) {
                LOGGER.log(
                        Level.WARNING,
                        "Помилка під час сповіщення "
                                + "спостерігача файлової системи.",
                        exception
                );
            }
        }
    }
}