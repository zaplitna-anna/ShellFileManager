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