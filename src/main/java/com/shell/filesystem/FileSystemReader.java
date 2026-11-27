package com.shell.filesystem;

import com.shell.model.FileSystemItem;
import com.shell.model.FileSystemItemType;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.shell.operation.CancellationToken;
import com.shell.strategy.search.SearchStrategy;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.util.Objects;

public class FileSystemReader {

    private static final Logger LOGGER =
            Logger.getLogger(
                    FileSystemReader.class.getName()
            );

    public List<Path> getAvailableDrives() {
        List<Path> drives = new ArrayList<>();

        for (Path root : FileSystems.getDefault()
                .getRootDirectories()) {

            drives.add(root);
        }

        return drives;
    }

    public List<FileSystemItem> listDirectory(
            Path directory
    ) throws IOException {

        List<FileSystemItem> items =
                new ArrayList<>();

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(directory)) {

            for (Path path : stream) {
                try {
                    items.add(getFileInfo(path));

                } catch (IOException exception) {

                    LOGGER.log(
                            Level.WARNING,
                            "Не вдалося отримати "
                                    + "інформацію про об'єкт: "
                                    + path,
                            exception
                    );
                }
            }

        } catch (DirectoryIteratorException exception) {
            throw exception.getCause();
        }

        return items;
    }


    public List<FileSystemItem> search(
            Path directory,
            String query,
            SearchStrategy strategy,
            CancellationToken cancellationToken
    ) throws IOException {

        Objects.requireNonNull(
                strategy,
                "Стратегія пошуку не може бути null."
        );

        Objects.requireNonNull(
                cancellationToken,
                "Токен скасування не може бути null."
        );

        cancellationToken.checkCancellation();

        if (query == null || query.isBlank()) {
            throw new IOException(
                    "Пошуковий запит не може бути порожнім."
            );
        }

        if (directory == null || !Files.isDirectory(directory)) {
            throw new IOException(
                    "Каталог пошуку не існує "
                            + "або не є каталогом."
            );
        }

        Path root = directory.toRealPath();

        if (!Files.isReadable(root)) {
            throw new AccessDeniedException(
                    root.toString()
            );
        }

        List<FileSystemItem> results =
                new ArrayList<>();

        Files.walkFileTree(
                root,
                new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path dir,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        cancellationToken.checkCancellation();

                        if (!dir.equals(root)) {

                            try {
                                if (attributes.isSymbolicLink()
                                        || attributes.isOther()
                                        || !dir.toRealPath().equals(dir)
                                        || !Files.isReadable(dir)) {

                                    return FileVisitResult.SKIP_SUBTREE;
                                }

                            } catch (IOException exception) {

                                LOGGER.log(
                                        Level.WARNING,
                                        "Пропущено недоступний каталог: "
                                                + dir,
                                        exception
                                );

                                return FileVisitResult.SKIP_SUBTREE;
                            }

                            addSearchResult(
                                    dir,
                                    query,
                                    strategy,
                                    results
                            );
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attributes
                    ) {

                        cancellationToken.checkCancellation();

                        if (attributes.isRegularFile()) {
                            addSearchResult(
                                    file,
                                    query,
                                    strategy,
                                    results
                            );
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(
                            Path file,
                            IOException exception
                    ) throws IOException {

                        cancellationToken.checkCancellation();

                        if (file.equals(root)) {
                            throw exception;
                        }

                        LOGGER.log(
                                Level.WARNING,
                                "Пропущено недоступний об'єкт: "
                                        + file,
                                exception
                        );

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(
                            Path dir,
                            IOException exception
                    ) throws IOException {

                        cancellationToken.checkCancellation();

                        if (exception != null) {

                            if (dir.equals(root)) {
                                throw exception;
                            }

                            LOGGER.log(
                                    Level.WARNING,
                                    "Не вдалося повністю переглянути "
                                            + "каталог: " + dir,
                                    exception
                            );
                        }

                        return FileVisitResult.CONTINUE;
                    }
                }
        );

        cancellationToken.checkCancellation();

        return results;
    }


    public FileSystemItem getFileInfo(Path path)
            throws IOException {

        Path normalizedPath =
                path.toAbsolutePath().normalize();

        BasicFileAttributes attributes =
                Files.readAttributes(
                        normalizedPath,
                        BasicFileAttributes.class
                );

        FileSystemItemType type =
                attributes.isDirectory()
                        ? FileSystemItemType.DIRECTORY
                        : FileSystemItemType.FILE;

        String name =
                normalizedPath.getFileName() == null
                        ? normalizedPath.toString()
                        : normalizedPath.getFileName().toString();

        String extension =
                attributes.isDirectory()
                        ? ""
                        : extractExtension(name);

        long size = attributes.isDirectory()
                ? -1
                : attributes.size();

        LocalDateTime lastModified =
                LocalDateTime.ofInstant(
                        attributes.lastModifiedTime().toInstant(),
                        ZoneId.systemDefault()
                );

        return new FileSystemItem(
                name,
                normalizedPath,
                type,
                extension,
                size,
                lastModified
        );
    }


    private void addSearchResult(
            Path path,
            String query,
            SearchStrategy strategy,
            List<FileSystemItem> results
    ) {

        String name = path.getFileName().toString();

        if (!strategy.matches(name, query)) {
            return;
        }

        try {
            results.add(getFileInfo(path));

        } catch (IOException exception) {

            LOGGER.log(
                    Level.WARNING,
                    "Не вдалося отримати інформацію "
                            + "про знайдений об'єкт: " + path,
                    exception
            );
        }
    }

    private String extractExtension(String name) {
        int dotIndex = name.lastIndexOf('.');

        if (dotIndex <= 0
                || dotIndex == name.length() - 1) {
            return "";
        }

        return name.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }
}