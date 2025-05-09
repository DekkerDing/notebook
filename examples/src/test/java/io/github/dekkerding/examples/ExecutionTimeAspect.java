package io.github.dekkerding.examples;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;

public class ExecutionTimeAspect implements BeforeEachCallback, AfterEachCallback {

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();


    /**
     * Callback that is invoked <em>after</em> an individual test and any
     * user-defined teardown methods for that test have been executed.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void afterEach(ExtensionContext context) {
        Method method = context.getRequiredTestMethod();
        if (method.isAnnotationPresent(ExecutionTime.class)) {
            long duration = System.currentTimeMillis() - START_TIME.get();
            System.out.println("方法 [" + method.getName() + "] 耗时: " + duration + " ms");
        }
        START_TIME.remove();
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
    }
}