package io.github.dekkerding.examples;

public class SynchronizedAlternatelyPrint {

    public static void main(String[] args) {
        final Object lock = new Object();
        final int[] count = new int[]{0};

        final Thread t1 = new Thread(() -> {
            for (; ; ) {
                synchronized (lock) {
                    if (count[0] == 10) {
                        lock.notifyAll();
                        break;
                    }
                    System.out.println("A");
                    count[0]++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    lock.notifyAll(); // 唤醒所有线程 唤醒 B 线程打印
                    try {
                        lock.wait(); // 阻塞当前线程 进入等待
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, "Print A");

        final Thread t2 = new Thread(() -> {
            for (; ; ) {
                synchronized (lock) {
                    if (count[0] == 10) {
                        lock.notifyAll();
                        break;
                    }
                    System.out.println("B");
                    count[0]++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    lock.notifyAll(); // 唤醒所有线程 唤醒 B 线程打印
                    try {
                        lock.wait(); // 释放锁 阻塞当前线程 进入等待
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        },"Print B");
        t1.start(); t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}