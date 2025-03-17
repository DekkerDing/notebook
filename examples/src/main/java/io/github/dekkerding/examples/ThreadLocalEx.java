package io.github.dekkerding.examples;

public class ThreadLocalEx {

    static ThreadLocal<String> threadLocal = new ThreadLocal<>();

    public static void main(String[] args) {
        Thread case1 = new  Thread(()->{
                for (int i = 0; i < 10; i++) {
                    threadLocal.set("case1 threadLocal " + i);
                    System.out.println(threadLocal.get());
                }
        });

        case1.start();

        Thread case2 = new Thread(()->{
                for (int i = 0; i < 10; i++) {
                    threadLocal.set("case2 threadLocal " + i);
                    System.out.println(threadLocal.get());
                }
        });

        case2.start();
    }

}