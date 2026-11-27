package com.shell.service;

import com.shell.command.CommandInvoker;
import com.shell.command.CreateFileCommand;
import com.shell.command.CreateDirectoryCommand;
import com.shell.command.RenameCommand;
import com.shell.facade.FileSystemFacade;
import com.shell.command.DeleteCommand;
import com.shell.command.OpenCommand;
import com.shell.command.CopyCommand;
import com.shell.command.MoveCommand;
import com.shell.observer.FileSystemEvent;
import com.shell.observer.FileSystemEventType;
import com.shell.observer.FileSystemEventPublisher;

import java.util.List;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class FileOperationService {

    private final FileSystemFacade fileSystemFacade;
    private final CommandInvoker commandInvoker;
    private final FileSystemEventPublisher eventPublisher;


    public FileOperationService(
            FileSystemFacade fileSystemFacade,
            CommandInvoker commandInvoker,
            FileSystemEventPublisher eventPublisher
    ) {
        this.fileSystemFacade = Objects.requireNonNull(
                fileSystemFacade,
                "Фасад файлової системи не може бути null."
        );

        this.commandInvoker = Objects.requireNonNull(
                commandInvoker,
                "Виконавець команд не може бути null."
        );

        this.eventPublisher = Objects.requireNonNull(
                eventPublisher,
                "Видавець подій не може бути null."
        );
    }


    public void createFile(
            Path directory,
            String name
    ) throws IOException {

        commandInvoker.executeCommand(
                new CreateFileCommand(
                        fileSystemFacade,
                        directory,
                        name
                )
        );

        eventPublisher.notifyObservers(
                new FileSystemEvent(
                        FileSystemEventType.CREATE,
                        null,
                        directory,
                        List.of(directory.resolve(name)),
                        null
                )
        );
    }


    public void createDirectory(
            Path directory,
            String name
    ) throws IOException {

        commandInvoker.executeCommand(
                new CreateDirectoryCommand(
                        fileSystemFacade,
                        directory,
                        name
                )
        );

        eventPublisher.notifyObservers(
                new FileSystemEvent(
                        FileSystemEventType.CREATE,
                        null,
                        directory,
                        List.of(directory.resolve(name)),
                        null
                )
        );
    }


    public void rename(
            Path source,
            String newName
    ) throws IOException {

        commandInvoker.executeCommand(
                new RenameCommand(
                        fileSystemFacade,
                        source,
                        newName
                )
        );

        Path newPath = source.resolveSibling(newName);

        eventPublisher.notifyObservers(
                new FileSystemEvent(
                        FileSystemEventType.RENAME,
                        source.getParent(),
                        newPath.getParent(),
                        List.of(source),
                        newPath
                )
        );
    }


    public void delete(List<Path> paths) throws IOException {

        List<Path> requestedPaths = List.copyOf(paths);

        Path sourceDirectory = requestedPaths.isEmpty()
                ? null
                : requestedPaths.getFirst().getParent();

        try {
            commandInvoker.executeCommand(
                    new DeleteCommand(
                            fileSystemFacade,
                            requestedPaths
                    )
            );

        } finally {
            eventPublisher.notifyObservers(
                    new FileSystemEvent(
                            FileSystemEventType.DELETE,
                            sourceDirectory,
                            null,
                            requestedPaths,
                            null
                    )
            );
        }
    }

    public void open(Path path) throws IOException {
        commandInvoker.executeCommand(
                new OpenCommand(
                        fileSystemFacade,
                        path
                )
        );
    }


    public void copy(
            List<Path> sourcePaths,
            Path destinationDirectory
    ) throws IOException {

        List<Path> requestedPaths =
                List.copyOf(sourcePaths);

        Path sourceDirectory = requestedPaths.isEmpty()
                ? null
                : requestedPaths.getFirst().getParent();

        try {
            commandInvoker.executeCommand(
                    new CopyCommand(
                            fileSystemFacade,
                            requestedPaths,
                            destinationDirectory
                    )
            );

        } finally {
            eventPublisher.notifyObservers(
                    new FileSystemEvent(
                            FileSystemEventType.COPY,
                            sourceDirectory,
                            destinationDirectory,
                            requestedPaths,
                            null
                    )
            );
        }
    }


    public void move(
            List<Path> sourcePaths,
            Path destinationDirectory
    ) throws IOException {

        List<Path> requestedPaths =
                List.copyOf(sourcePaths);

        Path sourceDirectory = requestedPaths.isEmpty()
                ? null
                : requestedPaths.getFirst().getParent();

        try {
            commandInvoker.executeCommand(
                    new MoveCommand(
                            fileSystemFacade,
                            requestedPaths,
                            destinationDirectory
                    )
            );

        } finally {
            eventPublisher.notifyObservers(
                    new FileSystemEvent(
                            FileSystemEventType.MOVE,
                            sourceDirectory,
                            destinationDirectory,
                            requestedPaths,
                            null
                    )
            );
        }
    }
}
