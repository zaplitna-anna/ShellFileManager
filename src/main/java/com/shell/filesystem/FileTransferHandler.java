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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileTransferHandler {

    private static final Logger LOGGER =
            Logger.getLogger(FileTransferHandler.class.getName());
    private static final int COPY_BUFFER_SIZE = 64 * 1024;

    private final FileSystemValidator validator;
    private final FileSystemWriter writer;

    public FileTransferHandler(
            FileSystemValidator validator,
            FileSystemWriter writer
    ) {
        this.validator = Objects.requireNonNull(
                validator, "Валідатор файлової системи не може бути null."
        );
        this.writer = Objects.requireNonNull(
                writer, "Компонент запису файлової системи не може бути null."
        );
    }

    public void copy(
            Path source,
            Path destinationDirectory,
            OperationContext context
    ) throws IOException {

        Objects.requireNonNull(
                context,
                "Контекст операції не може бути null."
        );

        context.checkCancellation();

        if (source == null || destinationDirectory == null) {
            throw new IOException(
                    "Не визначено шлях джерела "
                            + "або каталогу призначення."
            );
        }

        Path normalizedSource =
                source.toAbsolutePath().normalize();

        Path normalizedDestination =
                destinationDirectory
                        .toAbsolutePath()
                        .normalize();

        if (normalizedSource.getFileName() == null) {
            throw new IOException(
                    "Копіювання кореневого каталогу "
                            + "не підтримується."
            );
        }

        BasicFileAttributes attributes =
                Files.readAttributes(
                        normalizedSource,
                        BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS
                );

        if (!Files.isDirectory(normalizedDestination)) {
            throw new IOException(
                    "Каталог призначення не існує "
                            + "або не є каталогом."
            );
        }

        if (!Files.isReadable(normalizedSource)) {
            throw new AccessDeniedException(
                    normalizedSource.toString()
            );
        }

        if (!Files.isWritable(normalizedDestination)) {
            throw new AccessDeniedException(
                    normalizedDestination.toString()
            );
        }

        if (!attributes.isDirectory()
                && !attributes.isRegularFile()) {

            throw new IOException(
                    "Копіювання спеціальних об'єктів "
                            + "не підтримується."
            );
        }

        Path target = normalizedDestination.resolve(
                normalizedSource.getFileName()
        );

        if (attributes.isDirectory()
                && normalizedDestination.toRealPath()
                .startsWith(
                        normalizedSource.toRealPath()
                )) {

            throw new IOException(
                    "Неможливо копіювати каталог "
                            + "у самого себе або у його підкаталог."
            );
        }

        if (Files.exists(
                target,
                LinkOption.NOFOLLOW_LINKS
        )) {

            if (!Files.isSymbolicLink(target)
                    && Files.isSameFile(normalizedSource, target)) {

                throw new IOException(
                        "Джерело та призначення "
                                + "вказують на один об'єкт."
                );
            }

            throw new FileAlreadyExistsException(
                    target.toString()
            );
        }

        context.checkCancellation();

        if (attributes.isDirectory()) {
            validator.validateRegularDirectory(normalizedSource);

            copyDirectoryRecursively(
                    normalizedSource,
                    target,
                    context
            );

        } else {
            copyFileWithProgress(
                    normalizedSource,
                    target,
                    context
            );
        }
    }

    public void move(
            Path source,
            Path destinationDirectory,
            OperationContext context
    ) throws IOException {

        Objects.requireNonNull(
                context,
                "Контекст операції не може бути null."
        );

        context.checkCancellation();

        if (source == null || destinationDirectory == null) {
            throw new IOException(
                    "Не визначено шлях джерела "
                            + "або каталогу призначення."
            );
        }

        Path normalizedSource =
                source.toAbsolutePath().normalize();

        Path normalizedDestination =
                destinationDirectory
                        .toAbsolutePath()
                        .normalize();

        if (normalizedSource.getFileName() == null) {
            throw new IOException(
                    "Переміщення кореневого каталогу "
                            + "не підтримується."
            );
        }

        BasicFileAttributes attributes =
                Files.readAttributes(
                        normalizedSource,
                        BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS
                );

        if (!attributes.isDirectory()
                && !attributes.isRegularFile()) {

            throw new IOException(
                    "Переміщення спеціальних об'єктів "
                            + "не підтримується."
            );
        }

        if (!Files.isDirectory(normalizedDestination)) {
            throw new IOException(
                    "Каталог призначення не існує "
                            + "або не є каталогом."
            );
        }

        if (!Files.isWritable(normalizedDestination)) {
            throw new AccessDeniedException(
                    normalizedDestination.toString()
            );
        }

        if (attributes.isDirectory()) {

            validator.validateRegularDirectory(normalizedSource);

            if (normalizedDestination.toRealPath()
                    .startsWith(
                            normalizedSource.toRealPath()
                    )) {

                throw new IOException(
                        "Неможливо перемістити каталог "
                                + "у самого себе або у його підкаталог."
                );
            }
        }

        Path target = normalizedDestination.resolve(
                normalizedSource.getFileName()
        );

        if (Files.exists(
                target,
                LinkOption.NOFOLLOW_LINKS
        )) {

            if (!Files.isSymbolicLink(target)
                    && Files.isSameFile(normalizedSource, target)) {
                throw new IOException(
                        "Джерело та призначення "
                                + "вказують на один об'єкт."
                );
            }

            throw new FileAlreadyExistsException(
                    target.toString()
            );
        }

        FileStore sourceStore =
                Files.getFileStore(normalizedSource);

        FileStore destinationStore =
                Files.getFileStore(normalizedDestination);

        context.setCurrentPath(normalizedSource);
        context.checkCancellation();

        if (sourceStore.equals(destinationStore)) {
            Files.move(normalizedSource, target);
            context.addProcessedItems(1);

        } else {

            copy(
                    normalizedSource,
                    normalizedDestination,
                    context
            );

            context.checkCancellation();

            writer.delete(
                    normalizedSource,
                    context
            );
        }
    }

    private void copyDirectoryRecursively(
            Path source,
            Path target,
            OperationContext context
    ) throws IOException {

        Path realSource = source.toRealPath();

        Files.walkFileTree(
                source,
                new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path directory,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        context.checkCancellation();

                        Path relativePath =
                                source.relativize(directory);

                        Path expectedPath =
                                realSource.resolve(relativePath);

                        if (attributes.isSymbolicLink()
                                || attributes.isOther()
                                || !directory.toRealPath()
                                .equals(expectedPath)) {

                            throw new IOException(
                                    "Копіювання спеціальних "
                                            + "каталогів не підтримується: "
                                            + directory
                            );
                        }

                        context.setCurrentPath(directory);

                        Path targetDirectory =
                                target.resolve(relativePath);

                        Files.createDirectory(targetDirectory);

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        context.checkCancellation();

                        if (!attributes.isRegularFile()) {
                            throw new IOException(
                                    "Копіювання спеціальних "
                                            + "файлів не підтримується: "
                                            + file
                            );
                        }

                        Path relativePath =
                                source.relativize(file);

                        Path targetFile =
                                target.resolve(relativePath);

                        copyFileWithProgress(
                                file,
                                targetFile,
                                context
                        );

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
                            Path directory,
                            IOException exception
                    ) throws IOException {

                        if (exception != null) {
                            throw exception;
                        }

                        context.checkCancellation();

                        Path relativePath =
                                source.relativize(directory);

                        Path targetDirectory =
                                target.resolve(relativePath);

                        Files.setLastModifiedTime(
                                targetDirectory,
                                Files.getLastModifiedTime(directory)
                        );

                        context.setCurrentPath(directory);
                        context.addProcessedItems(1);

                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }

    private void copyFileWithProgress(
            Path source,
            Path target,
            OperationContext context
    ) throws IOException {

        context.checkCancellation();
        context.setCurrentPath(source);

        boolean targetCreated = false;

        try {
            try (InputStream inputStream =
                         Files.newInputStream(
                                 source,
                                 LinkOption.NOFOLLOW_LINKS
                         )) {

                try (OutputStream outputStream =
                             Files.newOutputStream(
                                     target,
                                     StandardOpenOption.CREATE_NEW,
                                     StandardOpenOption.WRITE
                             )) {

                    targetCreated = true;

                    byte[] buffer =
                            new byte[COPY_BUFFER_SIZE];

                    int bytesRead;

                    while (true) {
                        context.checkCancellation();

                        bytesRead = inputStream.read(buffer);

                        if (bytesRead == -1) {
                            break;
                        }

                        context.checkCancellation();

                        outputStream.write(
                                buffer,
                                0,
                                bytesRead
                        );

                        context.addProcessedBytes(bytesRead);
                    }
                }
            }

            context.checkCancellation();
            Files.setLastModifiedTime(
                    target,
                    Files.getLastModifiedTime(
                            source,
                            LinkOption.NOFOLLOW_LINKS
                    )
            );

        } catch (IOException | RuntimeException exception) {

            if (targetCreated) {
                try {
                    Files.deleteIfExists(target);

                } catch (IOException
                         | SecurityException cleanupException) {

                    exception.addSuppressed(cleanupException);

                    LOGGER.log(
                            Level.WARNING,
                            "Не вдалося видалити "
                                    + "незавершену копію файлу: "
                                    + target,
                            cleanupException
                    );
                }
            }

            throw exception;
        }

        context.addProcessedItems(1);
    }
}
