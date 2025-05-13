package io.github.dekkerding.examples;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

public class MonitoringAspect implements BeforeEachCallback, AfterEachCallback {

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();
    private static final ThreadLocal<Long> START_MEMORY = new ThreadLocal<>();



    /**
     * Callback that is invoked <em>after</em> an individual test and any
     * user-defined teardown methods for that test have been executed.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void afterEach(ExtensionContext context) {

        if (START_TIME.get() == null || START_MEMORY.get() == null) {
            System.err.println("缺少起始时间或内存记录");
            return;
        }

        Method method = context.getRequiredTestMethod();
        if (method.isAnnotationPresent(Monitoring.class)) {
            long duration = System.currentTimeMillis() - START_TIME.get();

            // 获取内存使用情况
            long endMemory = getUsedMemoryFromEden();
            long memoryConsumption = endMemory - START_MEMORY.get();

            System.out.println("方法 [" + method.getName() + "] 耗时: " + duration + " ms");
            System.out.println("方法 [" + method.getName() + "] 内存消耗: " + String.format("%.2f", (double) memoryConsumption / (1024 * 1024)) + " MB");
        }
        START_TIME.remove();
        START_MEMORY.remove();

    }

    /**
     * Callback that is invoked <em>before</em> an individual test and any
     * user-defined setup methods for that test have been executed.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        START_TIME.set(System.currentTimeMillis());
        // 建议手动触发 GC（但不能保证立即生效）
        System.gc();
        try {
            Thread.sleep(30); // 等待 GC 尽可能完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        START_MEMORY.set(getUsedMemoryFromEden());
    }

    public static long getUsedMemoryFromEden() {
        return ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(bean -> bean.getName().contains("Eden"))
                .mapToLong(bean -> bean.getUsage().getUsed())
                .sum();
    }
}