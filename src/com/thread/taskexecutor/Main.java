package com.thread.taskexecutor;

import java.util.UUID;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        TaskExecutor taskExecutor = new GroupTaskExecutor(2);

        TaskGroup taskGroup1 = new TaskGroup(UUID.randomUUID());
        TaskGroup taskGroup2 = new TaskGroup(UUID.randomUUID());

        Task<String> task1 = new Task<>(UUID.randomUUID(), taskGroup1, TaskType.READ, () -> {
            for (int i = 0; i < 5; i++) {
                Thread.sleep(1000);
                System.out.println(Thread.currentThread().getName() + " - " + "TaskGroup1 Task1 - " + i);
            }
            return "com.thread.taskexecutor.Task 1 done";
        });
        Task<String> task2 = new Task<>(UUID.randomUUID(), taskGroup1, TaskType.READ, () -> {
            for (int i = 0; i < 5; i++) {
                Thread.sleep(1000);
                System.out.println(Thread.currentThread().getName() + " - " + "TaskGroup1 Task2 - " + i);
            }
            return "com.thread.taskexecutor.Task 2 done";
        });
        Task<String> task3 = new Task<>(UUID.randomUUID(), taskGroup2, TaskType.READ, () -> {
            for (int i = 0; i < 5; i++) {
                Thread.sleep(1000);
                System.out.println(Thread.currentThread().getName() + " - " + "TaskGroup2 Task3 - " + i);
            }
            return "com.thread.taskexecutor.Task 3 done";
        });
        Task<String> task4 = new Task<>(UUID.randomUUID(), taskGroup2, TaskType.READ, () -> {
            for (int i = 0; i < 5; i++) {
                Thread.sleep(1000);
                System.out.println(Thread.currentThread().getName() + " - " + "TaskGroup2 Task4 - " + i);
            }
            return "com.thread.taskexecutor.Task 4 done";
        });

        Future<String> future1 = taskExecutor.submitTask(task1);
        Future<String> future2 = taskExecutor.submitTask(task2);
        Future<String> future3 = taskExecutor.submitTask(task3);
        Future<String> future4 = taskExecutor.submitTask(task4);

        taskExecutor.shutdown();

        System.out.println(future1.get());
        System.out.println(future2.get());
        System.out.println(future3.get());
        System.out.println(future4.get());
    }
}