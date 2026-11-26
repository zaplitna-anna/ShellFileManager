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
}
