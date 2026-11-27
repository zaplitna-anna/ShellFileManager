package com.shell.facade;

import com.shell.model.FileSystemItem;
import com.shell.model.FileSystemItemType;
import com.shell.model.OperationWorkload;
import com.shell.operation.OperationContext;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileSystemFacade {

    private static final Logger LOGGER =
            Logger.getLogger(FileSystemFacade.class.getName());

    private static final int COPY_BUFFER_SIZE = 64 * 1024;

    public List<Path> getAvailableDrives() {
        List<Path> drives = new ArrayList<>();

        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            drives.add(root);
        }

        return drives;
    }

    public List<FileSystemItem> listDirectory(Path directory)
            throws IOException {

        List<FileSystemItem> items = new ArrayList<>();

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(directory)) {

            for (Path path : stream) {
                try {
                    items.add(getFileInfo(path));
                } catch (IOException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Не вдалося отримати інформацію про об'єкт: " + path,
                            e
                    );
                }
            }
        } catch (DirectoryIteratorException e) {
            throw e.getCause();
        }

        return items;
    }

    public FileSystemItem getFileInfo(Path path)
            throws IOException {

        path = path.toAbsolutePath().normalize();

        BasicFileAttributes attributes =
                Files.readAttributes(path, BasicFileAttributes.class);

        boolean directory = attributes.isDirectory();

        FileSystemItemType type = directory
                ? FileSystemItemType.DIRECTORY
                : FileSystemItemType.FILE;

        String name = path.getFileName() == null
                ? path.toString()
                : path.getFileName().toString();

        String extension = directory
                ? ""
                : extractExtension(name);

        long size = directory ? -1 : attributes.size();

        LocalDateTime lastModified = LocalDateTime.ofInstant(
                attributes.lastModifiedTime().toInstant(),
                ZoneId.systemDefault()
        );

        return new FileSystemItem(
                name,
                path,
                type,
                extension,
                size,
                lastModified
        );
    }


    public void open(Path path) throws IOException {
        if (path == null) {
            throw new IOException(
                    "Об'єкт для відкриття не вказано."
            );
        }

        Path normalizedPath =
                path.toAbsolutePath().normalize();

        if (Files.notExists(normalizedPath)) {
            throw new NoSuchFileException(
                    normalizedPath.toString()
            );
        }

        if (!Files.isRegularFile(normalizedPath)) {
            throw new IOException(
                    "Вибраний об'єкт не є звичайним файлом."
            );
        }

        if (!Files.isReadable(normalizedPath)) {
            throw new AccessDeniedException(
                    normalizedPath.toString()
            );
        }

        if (!Desktop.isDesktopSupported()) {
            throw new IOException(
                    "Відкриття файлів не підтримується системою."
            );
        }

        Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            throw new IOException(
                    "Система не підтримує операцію відкриття файлів."
            );
        }

        desktop.open(normalizedPath.toFile());
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
            validateRegularDirectory(normalizedSource);

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

            validateRegularDirectory(normalizedSource);

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

            delete(
                    normalizedSource,
                    context
            );
        }
    }


    public OperationWorkload calculateWorkload(
            List<Path> paths,
            OperationContext context
    ) throws IOException {

        Objects.requireNonNull(
                paths,
                "Список шляхів не може бути null."
        );

        Objects.requireNonNull(
                context,
                "Контекст операції не може бути null."
        );

        long totalBytes = 0;
        long totalItems = 0;

        for (Path path : paths) {
            context.checkCancellation();

            OperationWorkload workload =
                    calculatePathWorkload(path, context);

            totalBytes += workload.totalBytes();
            totalItems += workload.totalItems();
        }

        return new OperationWorkload(
                totalBytes,
                totalItems
        );
    }


    public OperationWorkload calculateMoveWorkload(
            List<Path> sourcePaths,
            Path destinationDirectory,
            OperationContext context
    ) throws IOException {

        Objects.requireNonNull(
                sourcePaths,
                "Список шляхів не може бути null."
        );

        Objects.requireNonNull(
                destinationDirectory,
                "Каталог призначення не може бути null."
        );

        Objects.requireNonNull(
                context,
                "Контекст операції не може бути null."
        );

        context.checkCancellation();

        FileStore destinationStore =
                Files.getFileStore(destinationDirectory);

        long totalItems = 0;

        for (Path source : sourcePaths) {

            context.checkCancellation();
            context.setCurrentPath(source);

            FileStore sourceStore =
                    Files.getFileStore(source);

            if (sourceStore.equals(destinationStore)) {
                totalItems++;
            } else {
                OperationWorkload workload =
                        calculatePathWorkload(
                                source,
                                context
                        );
                totalItems += workload.totalItems() * 2;
            }
        }

        return new OperationWorkload(
                0,
                totalItems
        );
    }


    private OperationWorkload calculatePathWorkload(
            Path path,
            OperationContext context
    ) throws IOException {

        long[] totalBytes = {0};
        long[] totalItems = {0};

        Files.walkFileTree(
                path,
                new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path directory,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        context.checkCancellation();
                        validateRegularDirectory(directory);
                        context.setCurrentPath(directory);
                        totalItems[0]++;

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        context.checkCancellation();
                        context.setCurrentPath(file);

                        totalItems[0]++;

                        if (attributes.isRegularFile()) {
                            totalBytes[0] += attributes.size();
                        }

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
                }
        );

        return new OperationWorkload(
                totalBytes[0],
                totalItems[0]
        );
    }


    private String extractExtension(String name) {
        int dotIndex = name.lastIndexOf('.');

        if (dotIndex <= 0 || dotIndex == name.length() - 1) {
            return "";
        }

        return name.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

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

    public Path getRealPath(Path path) throws IOException {
        return path.toRealPath();
    }


    public Path createFile(
            Path directory,
            String name
    ) throws IOException {
        Path target = resolveTargetPath(directory, name);
        return Files.createFile(target);
    }

    public Path createDirectory(
            Path directory,
            String name
    ) throws IOException {
        Path target = resolveTargetPath(directory, name);
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

        Path target = resolveTargetPath(parent, newName);

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
            validateRegularDirectory(normalizedPath);
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


    private void validateRegularDirectory(
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


    private Path resolveTargetPath(
            Path directory,
            String name
    ) throws IOException {

        if (directory == null) {
            throw new IOException(
                    "Каталог не вказано."
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
                    "Назва об'єкта містить недопустимі символи."
            );
        }

        if (name.matches(
                "(?i)(CON|PRN|AUX|NUL|COM[1-9¹²³]|LPT[1-9¹²³])(\\..*)?"
        )) {
            throw new IOException(
                    "Ця назва зарезервована операційною системою."
            );
        }

        return directory.toAbsolutePath()
                .normalize()
                .resolve(name);
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
