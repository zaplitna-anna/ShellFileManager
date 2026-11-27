package com.shell.service;

import com.shell.command.CommandInvoker;
import com.shell.command.CopyCommand;
import com.shell.command.CreateDirectoryCommand;
import com.shell.command.CreateFileCommand;
import com.shell.command.DeleteCommand;
import com.shell.command.FileCommand;
import com.shell.command.MoveCommand;
import com.shell.command.OpenCommand;
import com.shell.command.RenameCommand;
import com.shell.database.OperationHistoryRepository;
import com.shell.facade.FileSystemFacade;
import com.shell.model.OperationHistoryEntry;
import com.shell.model.OperationStatus;
import com.shell.observer.FileSystemEvent;
import com.shell.observer.FileSystemEventPublisher;
import com.shell.observer.FileSystemEventType;
import com.shell.operation.OperationContext;
import com.shell.util.OperationErrorFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FileOperationService {

    private static final Logger LOGGER =
            Logger.getLogger(FileOperationService.class.getName());

    private final OperationHistoryRepository historyRepository;
    private final FileSystemFacade fileSystemFacade;
    private final CommandInvoker commandInvoker;
    private final FileSystemEventPublisher eventPublisher;


    public FileOperationService(
            FileSystemFacade fileSystemFacade,
            CommandInvoker commandInvoker,
            FileSystemEventPublisher eventPublisher,
            OperationHistoryRepository historyRepository
    ) {
        this.historyRepository = Objects.requireNonNull(historyRepository,
                "Репозиторій журналу операцій не може бути null.");

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

        executeLogged(
                FileSystemEventType.CREATE,
                null,
                requestedPath(directory, name),
                null,
                "Створено файл.",
                () -> new CreateFileCommand(
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

        executeLogged(
                FileSystemEventType.CREATE,
                null,
                requestedPath(directory, name),
                null,
                "Створено каталог.",
                () -> new CreateDirectoryCommand(
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

        executeLogged(
                FileSystemEventType.RENAME,
                Objects.toString(source, null),
                requestedPath(
                        source == null ? null : source.getParent(),
                        newName
                ),
                null,
                "Об'єкт перейменовано.",
                () -> new RenameCommand(
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

    public void open(Path path) throws IOException {
        commandInvoker.executeCommand(
                new OpenCommand(
                        fileSystemFacade,
                        path
                )
        );
    }

    public void delete(
            List<Path> paths,
            OperationContext context
    ) throws IOException {

        List<Path> requestedPaths =
                List.copyOf(paths);

        Path sourceDirectory = requestedPaths.isEmpty()
                ? null
                : requestedPaths.getFirst().getParent();

        try {
            executeLogged(
                    FileSystemEventType.DELETE,
                    formatPaths(requestedPaths),
                    null,
                    context,
                    "Об'єкти видалено.",
                    () -> new DeleteCommand(
                            fileSystemFacade,
                            requestedPaths,
                            context
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

    public void copy(
            List<Path> sourcePaths,
            Path destinationDirectory,
            OperationContext context
    ) throws IOException {

        List<Path> requestedPaths =
                List.copyOf(sourcePaths);

        Path sourceDirectory = requestedPaths.isEmpty()
                ? null
                : requestedPaths.getFirst().getParent();

        try {
            executeLogged(
                    FileSystemEventType.COPY,
                    formatPaths(requestedPaths),
                    Objects.toString(destinationDirectory, null),
                    context,
                    "Об'єкти скопійовано.",
                    () -> new CopyCommand(
                            fileSystemFacade,
                            requestedPaths,
                            destinationDirectory,
                            context
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
            Path destinationDirectory,
            OperationContext context
    ) throws IOException {

        List<Path> requestedPaths =
                List.copyOf(sourcePaths);

        Path sourceDirectory = requestedPaths.isEmpty()
                ? null
                : requestedPaths.getFirst().getParent();

        try {
            executeLogged(
                    FileSystemEventType.MOVE,
                    formatPaths(requestedPaths),
                    Objects.toString(destinationDirectory, null),
                    context,
                    "Об'єкти переміщено.",
                    () -> new MoveCommand(
                            fileSystemFacade,
                            requestedPaths,
                            destinationDirectory,
                            context
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

    public List<OperationHistoryEntry> getOperationHistory() throws SQLException {
        return historyRepository.findAll();
    }

    private void executeLogged(
            FileSystemEventType type,
            String sourcePath,
            String destinationPath,
            OperationContext context,
            String successMessage,
            Supplier<FileCommand> commandSupplier
    ) throws IOException {
        try {
            commandInvoker.executeCommand(commandSupplier.get());
        } catch (IOException | RuntimeException exception) {
            boolean cancelled = exception instanceof CancellationException;
            boolean changed = context != null && context.hasFileSystemChanges();

            OperationStatus status = changed ? OperationStatus.PARTIAL
                    : cancelled ? OperationStatus.CANCELLED : OperationStatus.FAILED;

            String message = cancelled
                    ? "Операцію скасовано."
                    : OperationErrorFormatter.format(exception);
            if (changed) {
                message += " Частина змін залишилася у файловій системі.";
            }
            if (exception.getSuppressed().length > 0) {
                message += " Виникли додаткові помилки під час завершення операції.";
            }
            saveHistory(type, sourcePath, destinationPath, status, message);
            throw exception;
        }

        saveHistory(type, sourcePath, destinationPath,
                OperationStatus.SUCCESS, successMessage);
    }

    private void saveHistory(FileSystemEventType type, String sourcePath,
                             String destinationPath, OperationStatus status,
                             String message) {
        try {
            historyRepository.save(type, sourcePath, destinationPath, status, message);
        } catch (SQLException exception) {
            LOGGER.log(Level.WARNING,
                    "Не вдалося зберегти запис журналу файлових операцій.", exception);
        }
    }

    private String formatPaths(List<Path> paths) {
        return paths.stream().map(Path::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    // Зберігаємо навіть некоректну введену назву, не створюючи Path до перевірки.
    private String requestedPath(Path directory, String name) {
        if (directory == null) {
            return name;
        }
        String base = directory.toString();
        String separator = directory.getFileSystem().getSeparator();
        return base + (base.endsWith(separator) ? "" : separator) + name;
    }
}
