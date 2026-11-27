package com.shell.command;

import java.io.IOException;
import java.util.Objects;

public class CommandInvoker {

    public void executeCommand(
            FileCommand command
    ) throws IOException {

        Objects.requireNonNull(
                command,
                "Файлова команда не може бути null."
        );

        command.execute();
    }
}
