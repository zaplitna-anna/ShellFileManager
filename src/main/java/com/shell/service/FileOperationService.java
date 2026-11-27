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

import java.util.List;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class FileOperationService {

    private final FileSystemFacade fileSystemFacade;
    private final CommandInvoker commandInvoker;

    public FileOperationService(
            FileSystemFacade fileSystemFacade,
            CommandInvoker commandInvoker
    ) {
        this.fileSystemFacade = Objects.requireNonNull(
                fileSystemFacade,
                "Фасад файлової системи не може бути null."
        );

        this.commandInvoker = Objects.requireNonNull(
                commandInvoker,
                "Ініціатор команд не може бути null."
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
    }

    public void delete(List<Path> paths) throws IOException {
        commandInvoker.executeCommand(
                new DeleteCommand(
                        fileSystemFacade,
                        paths
                )
        );
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

        commandInvoker.executeCommand(
                new CopyCommand(
                        fileSystemFacade,
                        sourcePaths,
                        destinationDirectory
                )
        );
    }


    public void move(
            List<Path> sourcePaths,
            Path destinationDirectory
    ) throws IOException {

        commandInvoker.executeCommand(
                new MoveCommand(
                        fileSystemFacade,
                        sourcePaths,
                        destinationDirectory
                )
        );
    }
}
