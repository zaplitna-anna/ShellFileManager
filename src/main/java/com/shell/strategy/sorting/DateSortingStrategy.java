package com.shell.strategy.sorting;

import com.shell.model.FileSystemItem;

import java.util.Comparator;

public class DateSortingStrategy
        implements FileSortingStrategy {

    @Override
    public Comparator<FileSystemItem> getComparator() {
        return Comparator.comparing(
                FileSystemItem::getLastModified
        );
    }
}