package com.shell.util;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;

public final class OperationErrorFormatter {

    private OperationErrorFormatter() {
    }

    public static String format(Exception exception) {

        if (exception instanceof FileAlreadyExistsException e) {

            return withPath(
                    "Об'єкт із такою назвою вже існує",
                    e.getFile()
            );

        } else if (exception instanceof AccessDeniedException e) {

            return withPath(
                    "Недостатньо прав доступу",
                    e.getFile()
            );

        } else if (exception instanceof SecurityException) {

            return "Недостатньо прав доступу "
                    + "для виконання операції.";

        } else if (exception instanceof NoSuchFileException e) {

            return withPath(
                    "Файл або каталог не існує",
                    e.getFile()
            );

        } else if (exception instanceof DirectoryNotEmptyException e) {

            return withPath(
                    "Каталог не порожній",
                    e.getFile()
            );

        } else if (exception instanceof UnsupportedOperationException) {

            return "Ця операція не підтримується "
                    + "поточною системою.";

        } else if (exception instanceof InvalidPathException) {

            return "Вказано недопустиму назву або шлях.";

        } else if (exception instanceof IllegalArgumentException) {

            return "Вказано некоректні параметри "
                    + "файлової операції.";

        } else if (exception.getClass() == IOException.class
                && exception.getMessage() != null
                && !exception.getMessage().isBlank()) {

            return exception.getMessage();
        }

        return "Під час виконання файлової операції "
                + "виникла помилка.";
    }

    private static String withPath(
            String message,
            String path
    ) {

        return path == null || path.isBlank()
                ? message + "."
                : message + ": " + path;
    }
}
