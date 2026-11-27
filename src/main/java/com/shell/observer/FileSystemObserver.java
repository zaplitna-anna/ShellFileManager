package com.shell.observer;

@FunctionalInterface
public interface FileSystemObserver {

    void onFileSystemChanged(FileSystemEvent event);
}