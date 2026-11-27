package com.shell.filesystem;

import com.shell.operation.OperationContext;
import com.shell.model.OperationWorkload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.nio.file.FileStore;
import java.util.List;

public class WorkloadCalculator {

    private final FileSystemValidator validator;

    public WorkloadCalculator(FileSystemValidator validator) {
        this.validator = Objects.requireNonNull(
                validator, "Валідатор файлової системи не може бути null."
        );
    }

    public OperationWorkload calculateWorkload(
            List<Path> paths,
            OperationContext context
    ) throws IOException {

        Objects.requireNonNull(
                paths,
                "Список шляхів не може бути null."
        );

        Objects.requireNonNull(
                context,
                "Контекст операції не може бути null."
        );

        long totalBytes = 0;
        long totalItems = 0;

        for (Path path : paths) {
            context.checkCancellation();

            OperationWorkload workload =
                    calculatePathWorkload(path, context);

            totalBytes += workload.totalBytes();
            totalItems += workload.totalItems();
        }

        return new OperationWorkload(
                totalBytes,
                totalItems
        );
    }


    public OperationWorkload calculateMoveWorkload(
            List<Path> sourcePaths,
            Path destinationDirectory,
            OperationContext context
    ) throws IOException {

        Objects.requireNonNull(
                sourcePaths,
                "Список шляхів не може бути null."
        );

        Objects.requireNonNull(
                destinationDirectory,
                "Каталог призначення не може бути null."
        );

        Objects.requireNonNull(
                context,
                "Контекст операції не може бути null."
        );

        context.checkCancellation();

        FileStore destinationStore =
                Files.getFileStore(destinationDirectory);

        long totalBytes = 0;
        long totalItems = 0;

        for (Path source : sourcePaths) {

            context.checkCancellation();
            context.setCurrentPath(source);

            FileStore sourceStore =
                    Files.getFileStore(source);

            if (sourceStore.equals(destinationStore)) {
                totalItems++;

            } else {
                OperationWorkload workload =
                        calculatePathWorkload(
                                source,
                                context
                        );

                totalBytes += workload.totalBytes();
                totalItems += workload.totalItems() * 2;
            }
        }

        return new OperationWorkload(
                totalBytes,
                totalItems
        );
    }


    private OperationWorkload calculatePathWorkload(
            Path path,
            OperationContext context
    ) throws IOException {

        Path normalizedPath =
                path.toAbsolutePath().normalize();

        long[] totalBytes = {0};
        long[] totalItems = {0};

        Files.walkFileTree(
                normalizedPath,
                new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path directory,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        context.checkCancellation();
                        validator.validateRegularDirectory(directory);
                        context.setCurrentPath(directory);
                        totalItems[0]++;

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        context.checkCancellation();
                        context.setCurrentPath(file);

                        totalItems[0]++;

                        if (attributes.isRegularFile()) {
                            totalBytes[0] += attributes.size();
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(
                            Path file,
                            IOException exception
                    ) throws IOException {

                        context.checkCancellation();

                        throw exception;
                    }
                }
        );

        return new OperationWorkload(
                totalBytes[0],
                totalItems[0]
        );
    }
}
