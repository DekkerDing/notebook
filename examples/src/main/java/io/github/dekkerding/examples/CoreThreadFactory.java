package io.github.dekkerding.examples;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CoreThreadFactory implements ThreadFactory  {

    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private static volatile AtomicReference<CoreThreadFactory> instance = new AtomicReference<>();

    CoreThreadFactory(String namePrefix) {
        SecurityManager sys = System.getSecurityManager();
        group = (sys != null) ? sys.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix + poolNumber.getAndIncrement() + "-thread-";
    }

    public static CoreThreadFactory getInstance(String namePrefix) {
        namePrefix = (namePrefix == null) ? "default" : namePrefix;
        // 使用 AtomicReference 简化双重检查锁定
        CoreThreadFactory current = instance.get();
        if (current == null) {
            synchronized (CoreThreadFactory.class) {
                current = instance.get();
                if (current == null) {
                    current = new CoreThreadFactory(namePrefix);
                    instance.set(current);
                }
            }
        }
        return current;
    }

    /**
     * Constructs a new {@code Thread}.  Implementations may also initialize
     * priority, name, daemon status, {@code ThreadGroup}, etc.
     *
     * @param r a runnable to be executed by new thread instance
     * @return constructed thread, or {@code null} if the request to
     * create a thread is rejected
     */
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(group, r,
                namePrefix + threadNumber.getAndIncrement(),
                0);
        if (thread.isDaemon())
            thread.setDaemon(false);
        if (thread.getPriority() != Thread.NORM_PRIORITY)
            thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
}