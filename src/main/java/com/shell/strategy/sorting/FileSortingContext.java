package com.shell.strategy.sorting;

import com.shell.model.FileSystemItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class FileSortingContext {

    private FileSortingStrategy strategy;
    private boolean ascending;

    public FileSortingContext() {
        strategy = new NameSortingStrategy();
        ascending = true;
    }

    public void setStrategy(FileSortingStrategy strategy) {
        this.strategy = Objects.requireNonNull(
                strategy,
                "Стратегія сортування не може бути null."
        );
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    public List<FileSystemItem> sort(
            List<FileSystemItem> items
    ) {
        List<FileSystemItem> sortedItems =
                new ArrayList<>(items);

        Comparator<FileSystemItem> comparator =
                strategy.getComparator();

        if (!ascending) {
            comparator = comparator.reversed();
        }

        Comparator<FileSystemItem> finalComparator =
                Comparator.comparingInt(
                                (FileSystemItem item) ->
                                        item.isDirectory() ? 0 : 1
                        )
                        .thenComparing(comparator)
                        .thenComparing(
                                FileSystemItem::getName,
                                String.CASE_INSENSITIVE_ORDER
                        );

        sortedItems.sort(finalComparator);

        return sortedItems;
    }

    public FileSortingStrategy getStrategy() {
        return strategy;
    }

    public boolean isAscending() {
        return ascending;
    }
}