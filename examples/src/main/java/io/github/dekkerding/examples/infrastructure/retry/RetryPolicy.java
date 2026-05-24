package io.github.dekkerding.examples.infrastructure.retry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 重试策略值对象
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPolicy {

    @Builder.Default
    private int maxAttempts = 3;

    @Builder.Default
    private long initialInterval = 1000L;

    @Builder.Default
    private double backoffMultiplier = 2.0;

    /**
     * 计算第 n 次重试的等待时间
     */
    public long calculateDelay(int attemptNumber) {
        if (attemptNumber <= 0) {
            return initialInterval;
        }
        return (long) (initialInterval * Math.pow(backoffMultiplier, attemptNumber - 1));
    }

    public static RetryPolicy defaultPolicy() {
        return RetryPolicy.builder().build();
    }
}
