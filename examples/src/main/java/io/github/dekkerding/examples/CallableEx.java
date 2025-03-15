package io.github.dekkerding.examples;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class CallableEx implements Callable<String> {
    @Override
    public String call() throws Exception {
        Thread thread = Thread.currentThread(); // 获取当前线程对象
        System.out.println(thread.getName() + " is running Hello");
        return thread.getState().name();
    }

    public static void main(String[] args) throws Exception {
        CallableEx callableEx = new CallableEx();
        FutureTask<String> futureTask = new FutureTask<>(callableEx);
        Thread thread = new Thread(futureTask);
        thread.setName("callableEx");
        thread.start();
        System.out.println(futureTask.get());
    }
}