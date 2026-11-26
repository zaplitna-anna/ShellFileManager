package com.shell.strategy.sorting;

import com.shell.model.FileSystemItem;

import java.util.Comparator;

public class SizeSortingStrategy
        implements FileSortingStrategy {

    @Override
    public Comparator<FileSystemItem> getComparator() {
        return Comparator.comparingLong(
                FileSystemItem::getSize
        );
    }
}