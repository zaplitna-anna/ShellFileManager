package com.shell.facade;

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
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.awt.Desktop;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

public class FileSystemFacade {

    private static final Logger LOGGER =
            Logger.getLogger(FileSystemFacade.class.getName());

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

        return Files.move(normalizedSource, target);
    }


    public void delete(Path path) throws IOException {
        if (path == null) {
            throw new IOException(
                    "Об'єкт для видалення не вказано."
            );
        }

        Path normalizedPath =
                path.toAbsolutePath().normalize();

        if (normalizedPath.getParent() == null) {
            throw new IOException(
                    "Видалення кореневого каталогу неможливе."
            );
        }

        BasicFileAttributes attributes =
                Files.readAttributes(
                        normalizedPath,
                        BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS
                );

        if (attributes.isDirectory()) {
            deleteDirectoryRecursively(normalizedPath);
        } else {
            Files.delete(normalizedPath);
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
            Path directory
    ) throws IOException {

        Files.walkFileTree(
                directory,
                new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attrs
                    ) throws IOException {

                        Files.delete(file);

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(
                            Path file,
                            IOException exception
                    ) throws IOException {

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

                        Files.delete(dir);

                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }


}
