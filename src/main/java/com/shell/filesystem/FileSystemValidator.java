package com.shell.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class FileSystemValidator {

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    public boolean isReadable(Path path) {
        return Files.isReadable(path);
    }

    public boolean isWritable(Path path) {
        return Files.isWritable(path);
    }

    public boolean isSameFile(
            Path firstPath,
            Path secondPath
    ) throws IOException {

        return Files.isSameFile(
                firstPath,
                secondPath
        );
    }

    public Path getRealPath(Path path)
            throws IOException {

        return path.toRealPath();
    }

    public Path resolveTargetPath(
            Path directory,
            String name
    ) throws IOException {

        if (directory == null) {
            throw new IOException(
                    "Не визначено каталог."
            );
        }

        if (name == null || name.isBlank()) {
            throw new IOException(
                    "Назва об'єкта не може бути порожньою."
            );
        }

        if (name.equals(".")
                || name.equals("..")
                || name.matches(".*[\\\\/:*?\"<>|].*")
                || name.endsWith(".")
                || name.endsWith(" ")
                || name.chars().anyMatch(ch -> ch < 32)) {

            throw new IOException(
                    "Назва містить недопустимі символи "
                            + "або має некоректний формат."
            );
        }

        if (name.matches(
                "(?i)(CON|PRN|AUX|NUL|COM[1-9¹²³]"
                        + "|LPT[1-9¹²³])(\\..*)?"
        )) {

            throw new IOException(
                    "Вказано зарезервовану назву Windows."
            );
        }

        return directory
                .toAbsolutePath()
                .normalize()
                .resolve(name);
    }

    public void validateRegularDirectory(
            Path directory
    ) throws IOException {

        BasicFileAttributes attributes =
                Files.readAttributes(
                        directory,
                        BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS
                );

        if (!attributes.isDirectory()
                || attributes.isSymbolicLink()
                || attributes.isOther()) {

            throw new IOException(
                    "Обробка спеціального каталогу "
                            + "не підтримується: "
                            + directory
            );
        }

        Path parent = directory.getParent();

        if (parent != null) {

            Path expectedPath = parent.toRealPath()
                    .resolve(directory.getFileName());

            if (!directory.toRealPath()
                    .equals(expectedPath)) {

                throw new IOException(
                        "Обробка перенаправленого каталогу "
                                + "не підтримується: "
                                + directory
                );
            }
        }
    }
}