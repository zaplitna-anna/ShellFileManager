package com.shell.facade;

import com.shell.filesystem.FileSystemReader;
import com.shell.filesystem.FileSystemValidator;
import com.shell.filesystem.FileSystemWriter;
import com.shell.filesystem.FileTransferHandler;
import com.shell.filesystem.WorkloadCalculator;
import com.shell.model.FileSystemItem;
import com.shell.model.OperationWorkload;
import com.shell.operation.OperationContext;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

public class FileSystemFacade {

    private final FileSystemReader reader;
    private final FileSystemValidator validator;
    private final FileSystemWriter writer;
    private final FileTransferHandler transferHandler;
    private final WorkloadCalculator workloadCalculator;

    public FileSystemFacade() {
        reader = new FileSystemReader();
        validator = new FileSystemValidator();
        writer = new FileSystemWriter(validator);
        transferHandler = new FileTransferHandler(validator, writer);
        workloadCalculator = new WorkloadCalculator(validator);
    }

    public List<Path> getAvailableDrives() {
        return reader.getAvailableDrives();
    }

    public List<FileSystemItem> listDirectory(
            Path directory
    ) throws IOException {
        return reader.listDirectory(directory);
    }

    public FileSystemItem getFileInfo(Path path)
            throws IOException {

        return reader.getFileInfo(path);
    }

    public boolean exists(Path path) {
        return validator.exists(path);
    }

    public boolean isDirectory(Path path) {
        return validator.isDirectory(path);
    }

    public boolean isReadable(Path path) {
        return validator.isReadable(path);
    }

    public boolean isWritable(Path path) {
        return validator.isWritable(path);
    }

    public boolean isSameFile(
            Path firstPath,
            Path secondPath
    ) throws IOException {

        return validator.isSameFile(
                firstPath,
                secondPath
        );
    }

    public Path getRealPath(Path path)
            throws IOException {

        return validator.getRealPath(path);
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

    public Path createFile(
            Path directory,
            String name
    ) throws IOException {
        return writer.createFile(directory, name);
    }

    public Path createDirectory(
            Path directory,
            String name
    ) throws IOException {
        return writer.createDirectory(directory, name);
    }

    public Path rename(
            Path source,
            String newName
    ) throws IOException {
        return writer.rename(source, newName);
    }

    public void delete(
            Path path,
            OperationContext context
    ) throws IOException {
        writer.delete(path, context);
    }

    public void copy(
            Path source,
            Path destinationDirectory,
            OperationContext context
    ) throws IOException {
        transferHandler.copy(source, destinationDirectory, context);
    }

    public void move(
            Path source,
            Path destinationDirectory,
            OperationContext context
    ) throws IOException {
        transferHandler.move(source, destinationDirectory, context);
    }

    public OperationWorkload calculateWorkload(
            List<Path> paths,
            OperationContext context
    ) throws IOException {
        return workloadCalculator.calculateWorkload(paths, context);
    }

    public OperationWorkload calculateMoveWorkload(
            List<Path> sourcePaths,
            Path destinationDirectory,
            OperationContext context
    ) throws IOException {
        return workloadCalculator.calculateMoveWorkload(sourcePaths, destinationDirectory, context);
    }


    public void validateTransfer(
            List<Path> sourcePaths,
            Path destinationDirectory,
            OperationContext context
    ) throws IOException {

        transferHandler.validateTransfer(
                sourcePaths,
                destinationDirectory,
                context
        );
    }

    public void validateFreeSpace(
            Path destinationDirectory,
            long requiredBytes
    ) throws IOException {

        transferHandler.validateFreeSpace(
                destinationDirectory,
                requiredBytes
        );
    }


}
