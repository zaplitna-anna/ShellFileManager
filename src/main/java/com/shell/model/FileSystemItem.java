package com.shell.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class FileSystemItem {

    private final String name;
    private final Path path;
    private final FileSystemItemType type;
    private final String extension;
    private final long size;
    private final LocalDateTime lastModified;

    public FileSystemItem(
            String name,
            Path path,
            FileSystemItemType type,
            String extension,
            long size,
            LocalDateTime lastModified
    ) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.extension = extension;
        this.size = size;
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public FileSystemItemType getType() {
        return type;
    }

    public String getExtension() {
        return extension;
    }

    public long getSize() {
        return size;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public boolean isDirectory() {
        return type == FileSystemItemType.DIRECTORY;
    }
}