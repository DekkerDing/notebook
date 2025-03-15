package io.github.dekkerding.examples;

// 消费者
public class Consumer implements Runnable {
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
                String food = Broker.queue.take();
                System.out.println(Thread.currentThread().getName()+":"+food);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
//    @Override
//    public void run() {
//        /**
//         * 1. 循环
//         * 2. 同步代码块
//         * 3. 判断共享数据是否到了末尾（到了末尾）
//         * 4. 判断共享数据是否到了末尾（没有到末尾，执行核心逻辑）
//         */
//        while (true) {
//            synchronized (Broker.desk) {
//                if (Broker.foods == 0) {
//                    // 等待
//                    try {
//                        Broker.desk.wait(3000);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }else {
//                    System.out.println(Thread.currentThread().getName()+"享用美食");
//                    Broker.foods = 0;
//                    Broker.desk.notifyAll();
//                }
//            }
//        }
//    }
}