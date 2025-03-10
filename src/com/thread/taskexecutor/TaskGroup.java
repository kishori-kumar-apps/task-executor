package com.thread.taskexecutor;

import java.util.UUID;

/**
 * com.thread.taskexecutor.Task group.
 *
 * @param groupUUID Unique group identifier.
 */
public record TaskGroup(
        UUID groupUUID
) {
    public TaskGroup {
        if (groupUUID == null) {
            throw new IllegalArgumentException("parameters must not be null");
        }
    }
}