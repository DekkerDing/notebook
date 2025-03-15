package io.github.dekkerding.examples;

public class RunnableEx implements Runnable{
    @Override
    public void run() {
        Thread thread = Thread.currentThread(); // 获取当前线程对象
        System.out.println(thread.getName() + " is running Hello");
    }

    public static void main(String[] args) {
        Thread runnableEx = new Thread( new RunnableEx());
        runnableEx.setName("runnableEx");
        runnableEx.start();
    }
}