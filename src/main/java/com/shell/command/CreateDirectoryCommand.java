package com.shell.command;

import com.shell.facade.FileSystemFacade;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class CreateDirectoryCommand implements FileCommand {

    private final FileSystemFacade fileSystemFacade;
    private final Path directory;
    private final String name;

    public CreateDirectoryCommand(
            FileSystemFacade fileSystemFacade,
            Path directory,
            String name
    ) {
        this.fileSystemFacade = Objects.requireNonNull(
                fileSystemFacade,
                "Фасад файлової системи не може бути null."
        );

        this.directory = directory;
        this.name = name;
    }

    @Override
    public void execute() throws IOException {
        fileSystemFacade.createDirectory(directory, name);
    }

    @Override
    public String getDescription() {
        return "Створення каталогу: " + name;
    }
}
