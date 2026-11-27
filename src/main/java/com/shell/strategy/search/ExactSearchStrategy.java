package com.shell.strategy.search;

public class ExactSearchStrategy
        implements SearchStrategy {

    @Override
    public boolean matches(
            String name,
            String query
    ) {
        return name.equalsIgnoreCase(query);
    }
}
