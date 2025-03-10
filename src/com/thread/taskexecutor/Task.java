package com.thread.taskexecutor;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Representation of computation to be performed by the {@link TaskExecutor}.
 *
 * @param taskUUID Unique task identifier.
 * @param taskGroup com.thread.taskexecutor.Task group.
 * @param taskType com.thread.taskexecutor.Task type.
 * @param taskAction Callable representing task computation and returning the result.
 * @param <T> com.thread.taskexecutor.Task computation result value type.
 */
public record Task<T>(
        UUID taskUUID,
        TaskGroup taskGroup,
        TaskType taskType,
        Callable<T> taskAction
) {
    public Task {
        if (taskUUID == null || taskGroup == null || taskType == null ||
                taskAction == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
    }
}