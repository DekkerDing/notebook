package io.github.dekkerding.examples;

import java.util.concurrent.*;

// 中间商
public class Broker {

    /**
     *  以 厨师 做面条 食客等待 享用面条为案例
     *  设置 一个出餐变量
     */
    // 0： 没有做好 1：已经做好
    public static int foods = 0;
    /**
     *  定义一个桌子变量 目的 看看有没有空位用餐 (锁对象)
     */
    public static Broker desk = new Broker();
    /**
     * 基于阻塞队列形式管理任务
     */
    public static ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(1);

    public static void main(String[] args) throws InterruptedException {

//        Thread producer = new Thread( new Producer());
//        producer.setName("厨师");
//        producer.start();
//
//        Thread consumer = new Thread( new Consumer());
//        consumer.setName("食客");
//        consumer.start();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, // 核心线程数
                10, // 最大线程数
                5000, // 线程空闲时间
                TimeUnit.MILLISECONDS, // 时间单位
                new LinkedBlockingQueue<Runnable>(), // 任务队列
                new CoreThreadFactory("core-thread"), // 线程工厂
                new ThreadPoolExecutor.AbortPolicy() // 拒绝策略
        );
        executor.submit(new Producer());
        executor.submit(new Consumer());
    }
}