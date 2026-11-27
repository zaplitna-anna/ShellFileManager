
package com.shell.operation;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CancellationToken {

    private final AtomicBoolean cancellationRequested =
            new AtomicBoolean(false);

    public void cancel() {
        cancellationRequested.set(true);
    }

    public boolean isCancellationRequested() {
        return cancellationRequested.get();
    }

    public void checkCancellation() {
        if (isCancellationRequested()) {
            throw new CancellationException(
                    "Файлову операцію скасовано."
            );
        }
    }
}