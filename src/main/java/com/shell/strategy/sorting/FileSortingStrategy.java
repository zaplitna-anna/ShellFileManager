package com.shell.strategy.sorting;

import com.shell.model.FileSystemItem;

import java.util.Comparator;

@FunctionalInterface
public interface FileSortingStrategy {

    Comparator<FileSystemItem> getComparator();
}