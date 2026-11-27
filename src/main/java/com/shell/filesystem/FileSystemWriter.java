package com.shell.filesystem;

import com.shell.operation.OperationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class FileSystemWriter {

    private final FileSystemValidator validator;

    public FileSystemWriter(FileSystemValidator validator) {
        this.validator = Objects.requireNonNull(
                validator, "Валідатор файлової системи не може бути null."
        );
    }

    public Path createFile(
            Path directory,
            String name
    ) throws IOException {
        Path target = validator.resolveTargetPath(directory, name);
        return Files.createFile(target);
    }

    public Path createDirectory(
            Path directory,
            String name
    ) throws IOException {
        Path target = validator.resolveTargetPath(directory, name);
        return Files.createDirectory(target);
    }

    public Path rename(
            Path source,
            String newName
    ) throws IOException {

        if (source == null) {
            throw new IOException(
                    "Об'єкт для перейменування не вказано."
            );
        }

        Path normalizedSource =
                source.toAbsolutePath().normalize();

        Path parent = normalizedSource.getParent();

        if (parent == null) {
            throw new IOException(
                    "Перейменування кореневого каталогу неможливе."
            );
        }

        Path target = validator.resolveTargetPath(parent, newName);

        if (normalizedSource.toString().equals(target.toString())) {
            return normalizedSource;
        }

        if (normalizedSource.equals(target)) {
            throw new IOException(
                    "Перейменування лише зі зміною регістру "
                            + "символів не підтримується."
            );
        }

        return Files.move(normalizedSource, target);
    }

    public void delete(
            Path path,
            OperationContext context
    ) throws IOException {

        Objects.requireNonNull(
                context,
                "Контекст операції не може бути null."
        );

        context.checkCancellation();

        if (path == null) {
            throw new IOException(
                    "Не визначено шлях до об'єкта."
            );
        }

        Path normalizedPath =
                path.toAbsolutePath().normalize();

        if (normalizedPath.getParent() == null) {
            throw new IOException(
                    "Видалення кореневого каталогу "
                            + "не підтримується."
            );
        }

        BasicFileAttributes attributes =
                Files.readAttributes(
                        normalizedPath,
                        BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS
                );

        if (attributes.isDirectory()) {
            validator.validateRegularDirectory(normalizedPath);
            deleteDirectoryRecursively(
                    normalizedPath,
                    context
            );

        } else {
            context.setCurrentPath(normalizedPath);
            context.checkCancellation();
            Files.delete(normalizedPath);
            context.addProcessedItems(1);
        }
    }

    private void deleteDirectoryRecursively(
            Path directory,
            OperationContext context
    ) throws IOException {

        Path realDirectory = directory.toRealPath();

        Files.walkFileTree(
                directory,
                new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path dir,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        context.checkCancellation();

                        Path relativePath =
                                directory.relativize(dir);

                        Path expectedPath =
                                realDirectory.resolve(relativePath);

                        if (attributes.isSymbolicLink()
                                || attributes.isOther()
                                || !dir.toRealPath()
                                .equals(expectedPath)) {

                            throw new IOException(
                                    "Рекурсивне видалення "
                                            + "спеціального каталогу "
                                            + "не підтримується: "
                                            + dir
                            );
                        }

                        context.setCurrentPath(dir);

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        context.checkCancellation();
                        context.setCurrentPath(file);

                        Files.delete(file);

                        context.addProcessedItems(1);

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(
                            Path file,
                            IOException exception
                    ) throws IOException {

                        context.checkCancellation();

                        throw exception;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(
                            Path dir,
                            IOException exception
                    ) throws IOException {

                        if (exception != null) {
                            throw exception;
                        }

                        context.checkCancellation();
                        context.setCurrentPath(dir);

                        Files.delete(dir);

                        context.addProcessedItems(1);

                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }
}
