package com.shell.strategy.sorting;

import com.shell.model.FileSystemItem;

import java.util.Comparator;

public class TypeSortingStrategy
        implements FileSortingStrategy {

    @Override
    public Comparator<FileSystemItem> getComparator() {
        return Comparator.comparing(
                FileSystemItem::getExtension,
                String.CASE_INSENSITIVE_ORDER
        );
    }
}