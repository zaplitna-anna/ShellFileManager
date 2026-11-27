package com.shell.command;

import com.shell.facade.FileSystemFacade;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class DeleteCommand implements FileCommand {

    private final FileSystemFacade fileSystemFacade;
    private final List<Path> paths;

    public DeleteCommand(
            FileSystemFacade fileSystemFacade,
            List<Path> paths
    ) {
        this.fileSystemFacade = Objects.requireNonNull(
                fileSystemFacade,
                "Фасад файлової системи не може бути null."
        );

        this.paths = List.copyOf(
                Objects.requireNonNull(
                        paths,
                        "Список об'єктів не може бути null."
                )
        );
    }

    @Override
    public void execute() throws IOException {
        if (paths.isEmpty()) {
            throw new IOException(
                    "Не вибрано об'єкти для видалення."
            );
        }

        for (Path path : paths) {
            fileSystemFacade.delete(path);
        }
    }

    @Override
    public String getDescription() {
        return "Видалення об'єктів: " + paths;
    }
}
