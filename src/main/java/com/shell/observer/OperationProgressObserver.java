package com.shell.observer;

import com.shell.model.OperationProgress;

@FunctionalInterface
public interface OperationProgressObserver {

    void onProgressChanged(OperationProgress progress);
}