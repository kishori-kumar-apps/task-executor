package com.thread.taskexecutor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GroupTaskExecutor implements TaskExecutor {

    /**
     * A data structure to keep key value data
     * */
    private record Pair<K, V>(K key, V value) {
    }

    /**
     * A working thread represents, a thread of the thread pool
     * */
    private static class WorkingThread implements Runnable {
        private final Runnable threadWorkingStrategyRunnable;
        private final String name;

        public WorkingThread(Runnable threadWorkingStrategyRunnable, String name) {
            this.threadWorkingStrategyRunnable = threadWorkingStrategyRunnable;
            this.name = name;
        }

        @Override
        public void run() {
            threadWorkingStrategyRunnable.run();
        }

        public String getName() {
            return this.name;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            WorkingThread that = (WorkingThread) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }

    // com.thread.taskexecutor.Task Queue. Pair of com.thread.taskexecutor.Task object and future task
    private final BlockingQueue<Pair<Task<?>, Runnable>> taskBlockingQueue;

    // To keep track of currently running task group.
    // Value 0 means thread group is not available
    // Value 1 means thread group is available
    private final ConcurrentHashMap<String, AtomicInteger> runningTaskGroup;

    // Allowed concurrency level
    private final int allowedConcurrency;
    private final Lock taskQueueLock = new ReentrantLock(true);

    // Condition will be used to make sure, Tasks sharing the same com.thread.taskexecutor.TaskGroup should not run concurrently
    private final Condition taskQueueNextTaskCondition = taskQueueLock.newCondition();
    private final Set<WorkingThread> workingThreads;
    private final AtomicInteger shutDownFlag;

    // To keep reference of next task, if any task of same com.thread.taskexecutor.TaskGroup is already running
    private final AtomicReference<Pair<Task<?>, Runnable>> nextTaskPairRef;

    /**
     * Create object with default allowed concurrency level as available cores
     * Note :- we can assume one core is used by main thread so allowed concurrency
     * is counted as one less than available cores
     * */
    public GroupTaskExecutor() {
        this(Runtime.getRuntime().availableProcessors() - 1);
    }

    public GroupTaskExecutor(int allowedConcurrency) {
        taskBlockingQueue = new LinkedBlockingQueue<>();
        runningTaskGroup = new ConcurrentHashMap<>();
        this.allowedConcurrency = allowedConcurrency;
        workingThreads = new HashSet<>();
        shutDownFlag = new AtomicInteger(0);
        nextTaskPairRef = new AtomicReference<>();
        initializeExecutorThreadPool();
    }

    /**
     * Initialize working threads as per the allowed concurrency level
     * */
    private void initializeExecutorThreadPool() {
        for (int i = 0; i < this.allowedConcurrency; i++) {
            workingThreads.add(new WorkingThread(getTaskExecutionStrategy(), String.valueOf(i + 1)));
        }

        for (WorkingThread workingThread : workingThreads) {
            Thread t = new Thread(workingThread, workingThread.getName());
            t.start();
        }
    }

    private Runnable getTaskExecutionStrategy() {
        return () -> {
            while (shutDownFlag.get() != 1 || (!taskBlockingQueue.isEmpty() || nextTaskPairRef.get() != null)) {

                Pair<Task<?>, Runnable> taskPair = null;
                try {
                    taskPair = takeTaskPair();

                    if (taskPair != null) {
                        Runnable runnableTask = taskPair.value();
                        runnableTask.run();

                        updateRunningTaskGroup(taskPair.key().taskGroup(), Boolean.FALSE);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Take next task to be executed
     * */
    private Pair<Task<?>, Runnable> takeTaskPair() throws InterruptedException {
        taskQueueLock.lock();

        try {
            if (nextTaskPairRef.get() == null) {
                if (shutDownFlag.get() == 1 && taskBlockingQueue.isEmpty())
                    return null;

                Pair<Task<?>, Runnable> nextTaskPairObj = taskBlockingQueue.take();
                nextTaskPairRef.compareAndSet(null, nextTaskPairObj);
            }

            Pair<Task<?>, Runnable> pair = nextTaskPairRef.get();
            String string = pair.key().taskGroup().groupUUID().toString();
            if (runningTaskGroup.containsKey(string) && runningTaskGroup.get(string).get() == 1) {
                // If the related task group is already actively running, threads should wait
                // until other task of the corresponding task group is running
                taskQueueNextTaskCondition.await();
            }

            updateRunningTaskGroup(pair.key().taskGroup(), Boolean.TRUE);
            nextTaskPairRef.compareAndSet(nextTaskPairRef.get(), null);
            taskQueueNextTaskCondition.signalAll();
            return pair;
        } finally {
            taskQueueLock.unlock();
        }
    }

    /**
     * Update currently running task group. Whether any task of particular group is running or completed running
     * */
    private void updateRunningTaskGroup(TaskGroup taskGroup, Boolean status){
        taskQueueLock.lock();
        try {
            if (status) {
                runningTaskGroup.computeIfAbsent(taskGroup.groupUUID().toString(), (k) -> new AtomicInteger(0)).compareAndSet(0, 1);
            } else {
                runningTaskGroup.computeIfAbsent(taskGroup.groupUUID().toString(), (k) -> new AtomicInteger(1)).compareAndSet(1, 0);
            }
        } finally {
            taskQueueLock.unlock();
        }
    }

    /**
     * Accepts the task and returns future
     * */
    @Override
    public <T> Future<T> submitTask(Task<T> task) {
        if (task == null)
            throw new NullPointerException("com.thread.taskexecutor.Task cannot be null");

        if (shutDownFlag.get() == 1)
            throw new IllegalStateException("Executor is already shutdown");

        RunnableFuture<T> future = new FutureTask<T>(task.taskAction());
        taskBlockingQueue.offer(new Pair<>(task, future));
        return future;
    }

    @Override
    public void shutdown() {
        shutDownFlag.compareAndSet(0, 1);
    }


}