package com.shell.strategy.search;

import java.util.Locale;

public class PartialSearchStrategy
        implements SearchStrategy {

    @Override
    public boolean matches(
            String name,
            String query
    ) {
        return name.toLowerCase(Locale.ROOT)
                .contains(
                        query.toLowerCase(Locale.ROOT)
                );
    }
}
