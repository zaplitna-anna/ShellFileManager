package com.shell.command;

import java.io.IOException;

public interface FileCommand {

    void execute() throws IOException;

    String getDescription();
}