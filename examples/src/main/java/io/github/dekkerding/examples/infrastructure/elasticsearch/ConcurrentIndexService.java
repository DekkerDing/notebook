package io.github.dekkerding.examples.infrastructure.elasticsearch;

import io.github.dekkerding.examples.infrastructure.elasticsearch.ElasticsearchVectorStore.VectorDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 并发索引服务
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ConcurrentIndexService {

    @Autowired
    private ElasticsearchConfig config;

    @Autowired
    private ElasticsearchVectorStore vectorStore;

    private ExecutorService executorService;
    private BlockingQueue<Runnable> workQueue;

    /**
     * 初始化线程池
     */
    private synchronized ExecutorService getExecutorService() {
        if (executorService == null || executorService.isShutdown()) {
            int poolSize = config.getIndexOptimization().getThreadPoolSize();
            int queueCapacity = config.getIndexOptimization().getQueueCapacity();

            workQueue = new LinkedBlockingQueue<>(queueCapacity);
            executorService = new ThreadPoolExecutor(
                    poolSize,
                    poolSize,
                    60L,
                    TimeUnit.SECONDS,
                    workQueue,
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

            log.info("初始化索引线程池: poolSize={}, queueCapacity={}",
                    poolSize, queueCapacity);
        }

        return executorService;
    }

    /**
     * 提交索引任务
     *
     * @param documents 文档列表
     * @return 索引结果
     */
    public ElasticsearchVectorStore.BulkIndexResult submitIndexTask(List<VectorDocument> documents) {
        log.info("提交索引任务: count={}", documents.size());

        // 计算动态批量大小
        int bulkSize = calculateBulkSize(documents);

        // 将文档分批
        List<List<VectorDocument>> batches = partitionDocuments(documents, bulkSize);

        // 并发执行批量索引
        List<Future<ElasticsearchVectorStore.BulkIndexResult>> futures = new ArrayList<>();

        for (List<VectorDocument> batch : batches) {
            Future<ElasticsearchVectorStore.BulkIndexResult> future =
                    getExecutorService().submit(() -> indexWithRetry(batch, 3));

            futures.add(future);
        }

        // 合并结果
        return mergeResults(futures, documents.size());
    }

    /**
     * 计算动态批量大小
     */
    public int calculateBulkSize(List<VectorDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return config.getIndexOptimization().getBulkMinSize();
        }

        // 计算平均文档大小
        long totalSize = 0;
        int count = 0;

        for (VectorDocument doc : documents) {
            long docSize = estimateDocumentSize(doc);
            totalSize += docSize;
            count++;
        }

        int avgSize = count > 0 ? (int) (totalSize / count) : 1000;

        // 根据平均文档大小动态调整批量大小
        int minBulkSize = config.getIndexOptimization().getBulkMinSize();
        int maxBulkSize = config.getIndexOptimization().getBulkMaxSize();

        // 文档越大，批量越小
        int bulkSize = minBulkSize + (maxBulkSize - minBulkSize) * Math.max(0, (10000 - avgSize)) / 10000;

        // 边界检查
        bulkSize = Math.max(minBulkSize, Math.min(maxBulkSize, bulkSize));

        log.debug("动态批量大小: avgSize={}, bulkSize={}", avgSize, bulkSize);

        return bulkSize;
    }

    /**
     * 估算文档大小（字节）
     */
    private long estimateDocumentSize(VectorDocument doc) {
        long size = 0;

        if (doc.getContent() != null) {
            size += doc.getContent().length() * 2; // UTF-16
        }

        if (doc.getVector() != null) {
            size += doc.getVector().length * 4; // float
        }

        if (doc.getMetadata() != null) {
            size += doc.getMetadata().size() * 100; // 估算
        }

        return size;
    }

    /**
     * 分批文档
     */
    private List<List<VectorDocument>> partitionDocuments(List<VectorDocument> documents, int batchSize) {
        List<List<VectorDocument>> batches = new ArrayList<>();

        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            batches.add(documents.subList(i, end));
        }

        return batches;
    }

    /**
     * 索引并重试
     */
    private ElasticsearchVectorStore.BulkIndexResult indexWithRetry(List<VectorDocument> batch,
                                                                       int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ElasticsearchVectorStore.BulkIndexResult result = vectorStore.bulkIndex(batch);

                if (result.isSuccess()) {
                    return result;
                }

                log.warn("索引失败，重试: attempt={}/{}", attempt, maxRetries);

            } catch (Exception e) {
                log.error("索引异常: attempt={}/{}", attempt, maxRetries, e);

                if (attempt == maxRetries) {
                    ElasticsearchVectorStore.BulkIndexResult result = new ElasticsearchVectorStore.BulkIndexResult();
                    result.setSuccess(false);
                    result.setTotal(batch.size());
                    result.setFailedCount(batch.size());
                    result.setErrorMessage(e.getMessage());
                    return result;
                }
            }

            try {
                Thread.sleep(1000 * attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        ElasticsearchVectorStore.BulkIndexResult result = new ElasticsearchVectorStore.BulkIndexResult();
        result.setSuccess(false);
        result.setTotal(batch.size());
        result.setFailedCount(batch.size());
        result.setErrorMessage("重试次数耗尽");

        return result;
    }

    /**
     * 合并结果
     */
    private ElasticsearchVectorStore.BulkIndexResult mergeResults(
            List<Future<ElasticsearchVectorStore.BulkIndexResult>> futures, int total) {

        ElasticsearchVectorStore.BulkIndexResult merged = new ElasticsearchVectorStore.BulkIndexResult();
        merged.setTotal(total);
        merged.setSuccessCount(0);
        merged.setFailedCount(0);

        for (Future<ElasticsearchVectorStore.BulkIndexResult> future : futures) {
            try {
                ElasticsearchVectorStore.BulkIndexResult result = future.get(60, TimeUnit.SECONDS);

                merged.setSuccessCount(merged.getSuccessCount() + result.getSuccessCount());
                merged.setFailedCount(merged.getFailedCount() + result.getFailedCount());

                if (!result.isSuccess()) {
                    merged.setSuccess(false);
                }

            } catch (Exception e) {
                log.error("获取索引结果失败", e);
                merged.setSuccess(false);
            }
        }

        merged.setSuccess(merged.getFailedCount() == 0);

        log.info("并发索引完成: total={}, success={}, failed={}",
                merged.getTotal(), merged.getSuccessCount(), merged.getFailedCount());

        return merged;
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            log.info("索引线程池已关闭");
        }
    }
}
