package com.shell.command;

import com.shell.facade.FileSystemFacade;
import com.shell.model.OperationWorkload;
import com.shell.operation.OperationContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class CopyCommand implements FileCommand {

    private final FileSystemFacade fileSystemFacade;
    private final List<Path> sourcePaths;
    private final Path destinationDirectory;
    private final OperationContext context;

    public CopyCommand(
            FileSystemFacade fileSystemFacade,
            List<Path> sourcePaths,
            Path destinationDirectory,
            OperationContext context
    ) {
        this.fileSystemFacade = Objects.requireNonNull(
                fileSystemFacade,
                "Фасад файлової системи не може бути null."
        );

        this.sourcePaths = List.copyOf(
                Objects.requireNonNull(
                        sourcePaths,
                        "Список шляхів не може бути null."
                )
        );

        this.destinationDirectory = Objects.requireNonNull(
                destinationDirectory,
                "Каталог призначення не може бути null."
        );

        this.context = Objects.requireNonNull(
                context,
                "Контекст операції не може бути null."
        );
    }

    @Override
    public void execute() throws IOException {

        if (sourcePaths.isEmpty()) {
            throw new IOException(
                    "Не вибрано об'єкти для копіювання."
            );
        }

        try {
            context.checkCancellation();

            OperationWorkload workload =
                    fileSystemFacade.calculateWorkload(
                            sourcePaths,
                            context
                    );

            context.checkCancellation();

            context.setTotalWork(
                    workload.totalBytes(),
                    workload.totalItems()
            );

            for (Path source : sourcePaths) {

                context.checkCancellation();

                fileSystemFacade.copy(
                        source,
                        destinationDirectory,
                        context
                );
            }

        } finally {
            context.publishFinalProgress();
        }
    }

    @Override
    public String getDescription() {
        return "Копіювання об'єктів: "
                + sourcePaths
                + " → "
                + destinationDirectory;
    }
}
