package com.shell.observer;

import com.shell.model.OperationProgress;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OperationProgressPublisher {

    private static final Logger LOGGER =
            Logger.getLogger(
                    OperationProgressPublisher.class.getName()
            );

    private final CopyOnWriteArrayList<OperationProgressObserver>
            observers = new CopyOnWriteArrayList<>();

    public void addObserver(
            OperationProgressObserver observer
    ) {
        Objects.requireNonNull(
                observer,
                "Спостерігач не може бути null."
        );

        observers.addIfAbsent(observer);
    }

    public void removeObserver(
            OperationProgressObserver observer
    ) {
        observers.remove(observer);
    }

    public void notifyObservers(OperationProgress progress) {
        Objects.requireNonNull(
                progress,
                "Інформація про прогрес не може бути null."
        );

        for (OperationProgressObserver observer : observers) {
            try {
                observer.onProgressChanged(progress);

            } catch (RuntimeException exception) {
                LOGGER.log(
                        Level.WARNING,
                        "Помилка під час сповіщення "
                                + "спостерігача про прогрес.",
                        exception
                );
            }
        }
    }
}