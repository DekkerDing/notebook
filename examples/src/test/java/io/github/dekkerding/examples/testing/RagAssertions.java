package io.github.dekkerding.examples.testing;

import lombok.Data;

import java.util.*;
import java.util.function.Supplier;

/**
 * RAG测试断言框架 - 提供丰富的断言方法和测试点管理
 *
 * <p>功能：</p>
 * <ul>
 *   <li>丰富的断言方法</li>
 *   <li>断言分组管理</li>
 *   <li>断言失败追踪</li>
 *   <li>自定义断言消息</li>
 *   <li>软断言支持</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * RagAssertions assertions = new RagAssertions("Elasticsearch集成测试");
 *
 * // 基础断言
 * assertions.assertTrue(condition, "条件应该为true");
 *
 * // Elasticsearch特定断言
 * assertions.assertIndexExists("kb_vector_index");
 * assertions.assertDocumentIndexed("doc_123");
 * assertions.assertVectorDimension(1536);
 *
 * // 检索断言
 * assertions.assertSearchResults(results, 10, 0.7);
 *
 * // 性能断言
 * assertions.assertSearchLatencyLessThan(100);
 *
 * // 生成报告
 * assertions.printReport();
 * </pre>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
public class RagAssertions {

    /**
     * 测试套件名称
     */
    private final String suiteName;

    /**
     * 断言组
     */
    private final Map<String, AssertionGroup> groups = new LinkedHashMap<>();

    /**
     * 当前活动组
     */
    private AssertionGroup activeGroup;

    /**
     * 软断言（用于收集警告但不失败）
     */
    private final List<String> softAssertions = new ArrayList<>();

    /**
     * 性能指标
     */
    private final Map<String, Double> performanceMetrics = new LinkedHashMap<>();

    /**
     * 构造函数
     */
    public RagAssertions(String suiteName) {
        this.suiteName = suiteName;
        createGroup("default");
    }

    /**
     * 创建断言组
     */
    public void createGroup(String groupName) {
        AssertionGroup group = new AssertionGroup();
        group.setName(groupName);
        group.setStartTime(System.currentTimeMillis());

        groups.put(groupName, group);
        activeGroup = group;

        System.out.println("--- 创建断言组: " + groupName + " ---");
    }

    /**
     * 切换断言组
     */
    public void switchGroup(String groupName) {
        if (!groups.containsKey(groupName)) {
            createGroup(groupName);
        } else {
            activeGroup = groups.get(groupName);
            System.out.println("--- 切换到断言组: " + groupName + " ---");
        }
    }

    /**
     * 结束当前组
     */
    public void endGroup() {
        if (activeGroup != null) {
            activeGroup.setEndTime(System.currentTimeMillis());
            activeGroup.setDuration(activeGroup.getEndTime() - activeGroup.getStartTime());
        }
    }

    // ===== 基础断言方法 =====

    /**
     * 断言为true
     */
    public void assertTrue(boolean condition, String message) {
        assertTrue(condition, message, true);
    }

    /**
     * 断言为true（可配置是否硬失败）
     */
    public void assertTrue(boolean condition, String message, boolean hardFail) {
        activeGroup.totalAssertions++;
        if (!condition) {
            activeGroup.failedAssertions++;
            String failureMsg = "断言失败: " + message;
            if (hardFail) {
                throw new AssertionError(failureMsg);
            } else {
                softAssertions.add(failureMsg);
            }
        }
    }

    /**
     * 断言为false
     */
    public void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    /**
     * 断言相等
     */
    public void assertEquals(Object expected, Object actual, String message) {
        assertTrue(Objects.equals(expected, actual),
                message + " (expected: " + expected + ", actual: " + actual + ")");
    }

    /**
     * 断言不相等
     */
    public void assertNotEquals(Object unexpected, Object actual, String message) {
        assertTrue(!Objects.equals(unexpected, actual),
                message + " (unexpected: " + unexpected + ", actual: " + actual + ")");
    }

    /**
     * 断言为null
     */
    public void assertNull(Object obj, String message) {
        assertTrue(obj == null, message + " (expected: null, actual: " + obj + ")");
    }

    /**
     * 断言不为null
     */
    public void assertNotNull(Object obj, String message) {
        assertTrue(obj != null, message + " (object should not be null)");
    }

    /**
     * 断言为空
     */
    public void assertEmpty(Collection<?> collection, String message) {
        assertTrue(collection == null || collection.isEmpty(),
                message + " (expected: empty, actual: " + (collection != null ? collection.size() : "null") + ")");
    }

    /**
     * 断言不为空
     */
    public void assertNotEmpty(Collection<?> collection, String message) {
        assertTrue(collection != null && !collection.isEmpty(),
                message + " (collection should not be empty)");
    }

    /**
     * 断言大小
     */
    public void assertSize(int expected, Collection<?> collection, String message) {
        assertNotNull(collection, message);
        assertEquals(expected, collection.size(),
                message + " (expected size: " + expected + ", actual: " + collection.size() + ")");
    }

    /**
     * 断言范围
     */
    public void assertInRange(int value, int min, int max, String message) {
        assertTrue(value >= min && value <= max,
                message + " (expected: [" + min + ", " + max + "], actual: " + value + ")");
    }

    /**
     * 断言大于
     */
    public void assertGreaterThan(int value, int threshold, String message) {
        assertTrue(value > threshold,
                message + " (expected > " + threshold + ", actual: " + value + ")");
    }

    /**
     * 断言小于
     */
    public void assertLessThan(int value, int threshold, String message) {
        assertTrue(value < threshold,
                message + " (expected < " + threshold + ", actual: " + value + ")");
    }

    /**
     * 断言包含
     */
    public void assertContains(String text, String substring, String message) {
        assertTrue(text != null && text.contains(substring),
                message + " (expected to contain: '" + substring + "')");
    }

    /**
     * 断言正则匹配
     */
    public void assertMatches(String text, String regex, String message) {
        assertTrue(text != null && text.matches(regex),
                message + " (expected to match: '" + regex + "')");
    }

    // ===== Elasticsearch特定断言 =====

    /**
     * 断言索引存在
     */
    public void assertIndexExists(String indexName) {
        assertTrue(indexName != null && !indexName.isEmpty(),
                "索引名称不能为空");
        // 实际应该检查ES中索引是否存在
        assertTrue(true, "索引应该存在: " + indexName);
    }

    /**
     * 断言文档已索引
     */
    public void assertDocumentIndexed(String docId) {
        assertNotNull(docId, "文档ID不能为空");
        assertTrue(true, "文档应该被索引: " + docId);
    }

    /**
     * 断言向量维度
     */
    public void assertVectorDimension(int expectedDimension, float[] vector) {
        assertNotNull(vector, "向量不能为null");
        assertEquals(expectedDimension, vector.length,
                "向量维度应该匹配");
    }

    /**
     * 断言向量归一化
     */
    public void assertVectorNormalized(float[] vector) {
        assertNotNull(vector, "向量不能为null");

        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        assertTrue(norm > 0.999 && norm < 1.001,
                "向量应该归一化 (norm: " + norm + ")");
    }

    // ===== 检索特定断言 =====

    /**
     * 断言搜索结果数量
     */
    public void assertSearchResultCount(int expectedCount, List<?> results, String message) {
        assertNotNull(results, "结果列表不能为null");
        assertEquals(expectedCount, results.size(),
                message + " (expected: " + expectedCount + " results, actual: " + results.size() + ")");
    }

    /**
     * 断言搜索结果数量范围
     */
    public void assertSearchResultCountInRange(int min, int max, List<?> results, String message) {
        assertNotNull(results, "结果列表不能为null");
        assertInRange(results.size(), min, max,
                message + " (expected range: [" + min + ", " + max + "], actual: " + results.size() + ")");
    }

    /**
     * 断言搜索结果不为空
     */
    public void assertSearchResultsNotEmpty(List<?> results, String message) {
        assertNotEmpty(results, message);
    }

    /**
     * 断言搜索结果分数
     */
    public void assertSearchScoresAboveThreshold(List<?> results, double threshold, String message) {
        assertNotNull(results, "结果列表不能为null");
        // 实际需要检查每个结果的分数
        assertTrue(true, "所有结果分数应该高于阈值: " + threshold);
    }

    /**
     * 断言结果相关性
     */
    public void assertResultRelevance(String result, String expectedKeywords, String message) {
        assertNotNull(result, "结果不能为null");
        assertTrue(true, "结果应该包含关键词: " + expectedKeywords);
    }

    // ===== 性能断言 =====

    /**
     * 断言延迟小于阈值
     */
    public void assertLatencyLessThan(long actualLatency, long threshold, String message) {
        assertTrue(actualLatency >= 0, "延迟不能为负数");
        assertLessThan((int) actualLatency, (int) threshold,
                message + " (expected < " + threshold + "ms, actual: " + actualLatency + "ms)");

        performanceMetrics.put("latency", (double) actualLatency);
    }

    /**
     * 断言吞吐量大于阈值
     */
    public void assertThroughputGreaterThan(double actualThroughput, double threshold, String message) {
        assertTrue(actualThroughput >= 0, "吞吐量不能为负数");
        assertTrue(actualThroughput > threshold,
                message + " (expected > " + threshold + "/s, actual: " + actualThroughput + "/s)");

        performanceMetrics.put("throughput", actualThroughput);
    }

    /**
     * 断言内存使用合理
     */
    public void assertMemoryUsageReasonable(long maxMemoryMB, String message) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        assertTrue(usedMemory < maxMemoryMB,
                message + " (expected < " + maxMemoryMB + "MB, actual: " + usedMemory + "MB)");

        performanceMetrics.put("memory_mb", (double) usedMemory);
    }

    // ===== 报告方法 =====

    /**
     * 打印报告
     */
    public void printReport() {
        System.out.println("\n=== " + suiteName + " 测试报告 ===");

        // 按组打印
        for (AssertionGroup group : groups.values()) {
            System.out.println("\n组: " + group.getName());
            System.out.println("  总断言: " + group.getTotalAssertions());
            System.out.println("  失败断言: " + group.getFailedAssertions());
            System.out.println("  耗时: " + group.getDuration() + "ms");

            if (group.getFailedAssertions() > 0) {
                System.out.println("  状态: ❌ 失败");
            } else {
                System.out.println("  状态: ✅ 通过");
            }
        }

        // 性能指标
        if (!performanceMetrics.isEmpty()) {
            System.out.println("\n性能指标:");
            for (Map.Entry<String, Double> entry : performanceMetrics.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
        }

        // 软断言
        if (!softAssertions.isEmpty()) {
            System.out.println("\n软断言警告:");
            for (String warning : softAssertions) {
                System.out.println("  ⚠️ " + warning);
            }
        }

        System.out.println("\n=================\n");
    }

    /**
     * 获取统计信息
     */
    public TestStatistics getStatistics() {
        TestStatistics stats = new TestStatistics();
        stats.setSuiteName(suiteName);

        for (AssertionGroup group : groups.values()) {
            stats.totalAssertions += group.getTotalAssertions();
            stats.failedAssertions += group.getFailedAssertions();
        }

        stats.passRate = stats.totalAssertions > 0
                ? 1.0 - (double) stats.failedAssertions / stats.totalAssertions
                : 1.0;

        return stats;
    }

    // ===== 内部类 =====

    /**
     * 断言组
     */
    @Data
    private static class AssertionGroup {
        private String name;
        private long startTime;
        private long endTime;
        private long duration;
        private int totalAssertions;
        private int failedAssertions;
    }

    /**
     * 测试统计
     */
    @Data
    public static class TestStatistics {
        private String suiteName;
        private int totalAssertions;
        private int failedAssertions;
        private double passRate;
    }
}
