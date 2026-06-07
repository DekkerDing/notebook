package io.github.dekkerding.examples.infrastructure.elasticsearch;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 索引性能指标
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class IndexMetrics {

    private final MeterRegistry meterRegistry;

    private Counter indexSuccessCounter;
    private Counter indexFailureCounter;
    private Timer indexLatencyTimer;
    private Counter documentCounter;
    private Counter bulkOperationCounter;
    private Counter retryCounter;

    @Autowired
    public IndexMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }

    /**
     * 初始化指标
     */
    private void initializeMetrics() {
        indexSuccessCounter = Counter.builder("index.operations.success")
                .description("成功索引操作计数")
                .register(meterRegistry);

        indexFailureCounter = Counter.builder("index.operations.failure")
                .description("失败索引操作计数")
                .register(meterRegistry);

        indexLatencyTimer = Timer.builder("index.operations.latency")
                .description("索引操作延迟")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        documentCounter = Counter.builder("index.documents.total")
                .description("索引文档总数")
                .register(meterRegistry);

        bulkOperationCounter = Counter.builder("index.bulk.operations")
                .description("批量索引操作计数")
                .register(meterRegistry);

        retryCounter = Counter.builder("index.operations.retry")
                .description("索引重试次数")
                .register(meterRegistry);

        log.info("索引指标初始化完成");
    }

    /**
     * 记录成功索引
     */
    public void recordIndexSuccess() {
        indexSuccessCounter.increment();
    }

    /**
     * 记录失败索引
     */
    public void recordIndexFailure() {
        indexFailureCounter.increment();
    }

    /**
     * 记录索引延迟
     */
    public void recordIndexLatency(long milliseconds) {
        indexLatencyTimer.record(milliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录索引文档
     */
    public void recordDocument(int count) {
        documentCounter.increment(count);
    }

    /**
     * 记录批量操作
     */
    public void recordBulkOperation() {
        bulkOperationCounter.increment();
    }

    /**
     * 记录重试
     */
    public void recordRetry() {
        retryCounter.increment();
    }

    /**
     * 记录批量索引结果
     */
    public void recordBulkResult(ElasticsearchVectorStore.BulkIndexResult result) {
        if (result.isSuccess()) {
            recordIndexSuccess();
            recordDocument(result.getSuccessCount());
        } else {
            recordIndexFailure();
            recordDocument(result.getSuccessCount());
        }

        recordBulkOperation();
    }

    /**
     * 获取索引吞吐量
     */
    public double getThroughput() {
        double documentCount = documentCounter.count();
        double totalTime = indexLatencyTimer.totalTime(TimeUnit.SECONDS);

        return totalTime > 0 ? documentCount / totalTime : 0;
    }

    /**
     * 获取索引错误率
     */
    public double getErrorRate() {
        double success = indexSuccessCounter.count();
        double failure = indexFailureCounter.count();
        double total = success + failure;

        return total > 0 ? failure / total : 0;
    }
}
