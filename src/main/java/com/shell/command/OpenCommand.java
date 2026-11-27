
package com.shell.command;

import com.shell.facade.FileSystemFacade;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class OpenCommand implements FileCommand {

    private final FileSystemFacade fileSystemFacade;
    private final Path path;

    public OpenCommand(
            FileSystemFacade fileSystemFacade,
            Path path
    ) {
        this.fileSystemFacade = Objects.requireNonNull(
                fileSystemFacade,
                "Фасад файлової системи не може бути null."
        );

        this.path = Objects.requireNonNull(
                path,
                "Шлях до файлу не може бути null."
        );
    }

    @Override
    public void execute() throws IOException {
        fileSystemFacade.open(path);
    }

    @Override
    public String getDescription() {
        return "Відкриття файлу: " + path;
    }
}
