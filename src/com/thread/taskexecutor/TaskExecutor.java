package com.thread.taskexecutor;

import java.util.concurrent.Future;

public interface TaskExecutor {
    /**
     * Submit new task to be queued and executed.
     *
     * @param task com.thread.taskexecutor.Task to be executed by the executor. Must not be null.
     * @return Future for the task asynchronous computation result.
     */
    <T> Future<T> submitTask(Task<T> task);

    /**
     * Stop accepting new tasks and stop the thread pools, once all the submitted
     * tasks completed it's work
     * */
    void shutdown();
}