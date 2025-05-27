package io.github.dekkerding.examples;

import java.util.concurrent.Semaphore;

public class SemaphoreAlternatelyPrint {

    public static void main(String[] args) {

        /**
         *  使用三个线程交替打印 1 2 3
         */

        final Semaphore first = new Semaphore(0); // 零值信号量：条件变量，二值信号量： 互斥锁（不可重入）
        final Semaphore second = new Semaphore(0);
        final Semaphore third = new Semaphore(0);

        final Thread t1 = new Thread(() -> {
            for (; ; ) {
                try {
                    System.out.println("1");
                    Thread.sleep(1000);
                    second.release(); // 释放信号量 唤醒第二线程
                    first.acquire(); // 获取信号量，阻塞当前线程 休眠
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }, "Print 1");

        final Thread t2 = new Thread(() -> {
            for (; ; ) {
                try {
                    second.acquire();
                    System.out.println("2");
                    Thread.sleep(1000);
                    third.release();  // 释放信号量 唤醒第三线程
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }, "Print 2");

        final Thread t3 = new Thread(() -> {
            for (; ; ) {
                try {
                    third.acquire();
                    System.out.println("3");
                    Thread.sleep(1000);
                    first.release();  // 释放信号量 唤醒第一线程
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }, "Print 3");

        t1.start(); t2.start(); t3.start();
    }
}