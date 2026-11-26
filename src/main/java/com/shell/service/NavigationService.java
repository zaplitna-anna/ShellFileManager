package com.shell.service;

import com.shell.facade.FileSystemFacade;
import com.shell.model.FileSystemItem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class NavigationService {

    private final FileSystemFacade fileSystemFacade;

    public NavigationService(FileSystemFacade fileSystemFacade) {
        this.fileSystemFacade = Objects.requireNonNull(fileSystemFacade);
    }

    public List<Path> getAvailableDrives() {
        return fileSystemFacade.getAvailableDrives();
    }

    public List<FileSystemItem> getDirectoryContents(Path directory)
            throws IOException {

        validateDirectory(directory);

        return fileSystemFacade.listDirectory(directory);
    }

    public Path getParentDirectory(Path currentDirectory) {
        Path normalizedPath = currentDirectory
                .toAbsolutePath()
                .normalize();

        Path parent = normalizedPath.getParent();

        return parent != null ? parent : normalizedPath;
    }

    public Path getInitialDirectory() {
        return Path.of(System.getProperty("user.home"))
                .toAbsolutePath()
                .normalize();
    }

    private void validateDirectory(Path directory)
            throws IOException {

        if (directory == null) {
            throw new IOException("Шлях до каталогу не вказано.");
        }

        if (!fileSystemFacade.exists(directory)) {
            throw new IOException(
                    "Каталог не існує або недоступний: " + directory
            );
        }

        if (!fileSystemFacade.isDirectory(directory)) {
            throw new IOException(
                    "Вибраний об'єкт не є каталогом: " + directory
            );
        }

        if (!fileSystemFacade.isReadable(directory)) {
            throw new IOException(
                    "Немає прав доступу до каталогу: " + directory
            );
        }
    }
}