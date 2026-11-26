package com.shell.strategy.sorting;

import com.shell.model.FileSystemItem;

import java.util.Comparator;

public class NameSortingStrategy
        implements FileSortingStrategy {

    @Override
    public Comparator<FileSystemItem> getComparator() {
        return Comparator.comparing(
                FileSystemItem::getName,
                String.CASE_INSENSITIVE_ORDER
        );
    }
}