package io.github.dekkerding.examples;

public class ThreadEx extends Thread {

    @Override
    public void run() {
        System.out.println(getName()+" is running Hello");
    }

    public static void main(String[] args) {
        // TODO 注意 使用 Thread 类方式会创建多个对象
        // TODO (在运行的时候多个对象管理的线程共享资源有可能不是同一个 即 维护线程的锁对象资源也有可能不是同一个)
        ThreadEx threadEx = new ThreadEx();
        threadEx.setName("threadEx");
        threadEx.start();
    }
}