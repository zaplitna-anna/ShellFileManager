package com.shell.command;

import com.shell.facade.FileSystemFacade;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class RenameCommand implements FileCommand {

    private final FileSystemFacade fileSystemFacade;
    private final Path source;
    private final String newName;

    public RenameCommand(
            FileSystemFacade fileSystemFacade,
            Path source,
            String newName
    ) {
        this.fileSystemFacade = Objects.requireNonNull(
                fileSystemFacade,
                "Фасад файлової системи не може бути null."
        );

        this.source = source;
        this.newName = newName;
    }

    @Override
    public void execute() throws IOException {
        fileSystemFacade.rename(source, newName);
    }

    @Override
    public String getDescription() {
        return "Перейменування об'єкта: "
                + source
                + " → "
                + newName;
    }
}
