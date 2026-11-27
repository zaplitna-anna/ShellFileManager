package com.shell.command;

import com.shell.facade.FileSystemFacade;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class MoveCommand implements FileCommand {

    private final FileSystemFacade fileSystemFacade;
    private final List<Path> sourcePaths;
    private final Path destinationDirectory;

    public MoveCommand(
            FileSystemFacade fileSystemFacade,
            List<Path> sourcePaths,
            Path destinationDirectory
    ) {
        this.fileSystemFacade = Objects.requireNonNull(
                fileSystemFacade,
                "Фасад файлової системи не може бути null."
        );

        this.sourcePaths = List.copyOf(
                Objects.requireNonNull(
                        sourcePaths,
                        "Список об'єктів не може бути null."
                )
        );

        this.destinationDirectory = Objects.requireNonNull(
                destinationDirectory,
                "Каталог призначення не може бути null."
        );
    }

    @Override
    public void execute() throws IOException {
        if (sourcePaths.isEmpty()) {
            throw new IOException(
                    "Не вибрано об'єкти для переміщення."
            );
        }

        for (Path source : sourcePaths) {
            fileSystemFacade.move(
                    source,
                    destinationDirectory
            );
        }
    }

    @Override
    public String getDescription() {
        return "Переміщення об'єктів: "
                + sourcePaths
                + " → "
                + destinationDirectory;
    }
}