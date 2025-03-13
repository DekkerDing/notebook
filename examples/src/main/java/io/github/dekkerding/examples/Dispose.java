package io.github.dekkerding.examples;

import java.util.Random;
import java.util.concurrent.*;

/**
 *  JDK 1.8
 */
public class Dispose {

    private static final int THREAD_COUNT = 5;
    private static final int TASK_COUNT = 10; // N <= 100
    private static final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    private static final CountDownLatch latch = new CountDownLatch(TASK_COUNT);


    public static void main(String[] args) {
        // 创建并提交任务
        for (int i = 0; i < TASK_COUNT; i++) {
            int executionTime = new Random().nextInt(1000) + 100; // 随机执行时间100-1000毫秒
            Runnable task = () -> {
                try {
                    System.out.println("Task started: " + Thread.currentThread().getName());
                    Thread.sleep(executionTime);
                    System.out.println("Task completed: " + Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            };
            taskQueue.add(task);
        }

        // 启动工作线程
        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                while (true) {
                    try {
                        Runnable task = taskQueue.take();
                        task.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        // 等待所有任务完成
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 关闭线程池
        executorService.shutdown();
        System.out.println("All tasks completed. Program exiting.");
    }
}