package com.shell.command;

import com.shell.facade.FileSystemFacade;
import com.shell.model.OperationWorkload;
import com.shell.operation.OperationContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class DeleteCommand implements FileCommand {

    private final FileSystemFacade fileSystemFacade;
    private final List<Path> paths;
    private final OperationContext context;

    public DeleteCommand(
            FileSystemFacade fileSystemFacade,
            List<Path> paths,
            OperationContext context
    ) {
        this.fileSystemFacade = Objects.requireNonNull(
                fileSystemFacade,
                "Фасад файлової системи не може бути null."
        );

        this.paths = List.copyOf(
                Objects.requireNonNull(
                        paths,
                        "Список шляхів не може бути null."
                )
        );

        this.context = Objects.requireNonNull(
                context,
                "Контекст операції не може бути null."
        );
    }

    @Override
    public void execute() throws IOException {

        if (paths.isEmpty()) {
            throw new IOException(
                    "Не вибрано об'єкти для видалення."
            );
        }

        try {
            context.checkCancellation();

            OperationWorkload workload =
                    fileSystemFacade.calculateWorkload(
                            paths,
                            context
                    );

            context.checkCancellation();

            context.setTotalWork(
                    0,
                    workload.totalItems()
            );

            for (Path path : paths) {

                context.checkCancellation();

                fileSystemFacade.delete(
                        path,
                        context
                );
            }

        } finally {
            context.publishFinalProgress();
        }
    }

    @Override
    public String getDescription() {
        return "Видалення об'єктів: " + paths;
    }
}
