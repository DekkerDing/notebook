package io.github.dekkerding.examples;

import static io.github.dekkerding.examples.Broker.*;

// 生产者
public class Producer implements Runnable {
    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        while (true) {
            try {
                System.out.println(Thread.currentThread().getName() + "加工");
                queue.put("food");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 1. 循环
     * 2. 同步代码块
     * 3. 判断共享数据是否到了末尾（到了末尾）
     * 4. 判断共享数据是否到了末尾（没有到末尾，执行核心逻辑）
     */

//    @Override
//    public void run() {
//        while (true) {
//            synchronized (Broker.desk) {
//                if (Broker.foods == 1) {
//                    try {
//                        System.out.println(Thread.currentThread().getName()+"休息一下");
//                        Broker.desk.wait(3000);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }else {
//                    System.out.println(Thread.currentThread().getName()+"出餐");
//                    Broker.foods = 1;
//                    Broker.desk.notifyAll();
//                }
//            }
//        }
//    }

}