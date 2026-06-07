package io.github.dekkerding.examples.testing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试进度追踪器 - 实时追踪测试执行进度和状态
 *
 * <p>功能：</p>
 * <ul>
 *   <li>追踪测试套件执行进度</li>
 *   <li>记录测试点执行状态</li>
 *   <li>统计测试通过率</li>
 *   <li>生成测试报告</li>
 *   <li>性能指标收集</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * TestProgressTracker tracker = TestProgressTracker.getInstance("RAG集成测试");
 *
 * // 开始测试套件
 * tracker.startSuite("Elasticsearch集成测试");
 *
 * // 执行测试点
 * tracker.executeTest("索引创建", () -> {
 *     assertTrue(esService.createIndex());
 * });
 *
 * // 生成报告
 * TestReport report = tracker.generateReport();
 * </pre>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
public class TestProgressTracker {

    /**
     * 单例实例
     */
    private static final Map<String, TestProgressTracker> instances = new ConcurrentHashMap<>();

    /**
     * 测试套件名称
     */
    private final String suiteName;

    /**
     * 开始时间
     */
    private LocalDateTime suiteStartTime;

    /**
     * 结束时间
     */
    private LocalDateTime suiteEndTime;

    /**
     * 总测试数
     */
    private final AtomicInteger totalTests = new AtomicInteger(0);

    /**
     * 通过测试数
     */
    private final AtomicInteger passedTests = new AtomicInteger(0);

    /**
     * 失败测试数
     */
    private final AtomicInteger failedTests = new AtomicInteger(0);

    /**
     * 跳过测试数
     */
    private final AtomicInteger skippedTests = new AtomicInteger(0);

    /**
     * 测试点记录
     */
    private final Map<String, TestPointResult> testPoints = new ConcurrentHashMap<>();

    /**
     * 当前阶段
     */
    private String currentPhase;

    /**
     * 阶段开始时间
     */
    private LocalDateTime phaseStartTime;

    /**
     * 性能指标收集
     */
    private final Map<String, PerformanceMetric> performanceMetrics = new ConcurrentHashMap<>();

    /**
     * 获取实例
     */
    public static TestProgressTracker getInstance(String suiteName) {
        return instances.computeIfAbsent(suiteName, TestProgressTracker::new);
    }

    /**
     * 构造函数
     */
    private TestProgressTracker(String suiteName) {
        this.suiteName = suiteName;
    }

    /**
     * 开始测试套件
     */
    public void startSuite() {
        this.suiteStartTime = LocalDateTime.now();
        log("=== 开始测试套件: {} ===", suiteName);
        log("开始时间: {}", suiteStartTime);
    }

    /**
     * 结束测试套件
     */
    public void endSuite() {
        this.suiteEndTime = LocalDateTime.now();
        log("=== 测试套件完成: {} ===", suiteName);
        log("结束时间: {}", suiteEndTime);
        log("总耗时: {} ms", getDuration().toMillis());
    }

    /**
     * 开始阶段
     */
    public void startPhase(String phaseName) {
        this.currentPhase = phaseName;
        this.phaseStartTime = LocalDateTime.now();
        log("--- 开始阶段: {} ---", phaseName);
    }

    /**
     * 结束阶段
     */
    public void endPhase() {
        if (currentPhase != null && phaseStartTime != null) {
            long duration = Duration.between(phaseStartTime, LocalDateTime.now()).toMillis();
            log("--- 阶段完成: {} (耗时: {} ms) ---", currentPhase, duration);
        }
        this.currentPhase = null;
        this.phaseStartTime = null;
    }

    /**
     * 执行测试点
     */
    public void executeTest(String testName, TestExecutor executor) {
        executeTest(testName, executor, TestPriority.NORMAL);
    }

    /**
     * 执行测试点（带优先级）
     */
    public void executeTest(String testName, TestExecutor executor, TestPriority priority) {
        totalTests.incrementAndGet();
        LocalDateTime startTime = LocalDateTime.now();

        TestPointResult result = new TestPointResult();
        result.setTestName(testName);
        result.setStartTime(startTime);
        result.setPriority(priority);
        result.setPhase(currentPhase);

        try {
            // 执行测试
            executor.execute();

            // 测试通过
            result.setStatus(TestStatus.PASSED);
            result.setEndTime(LocalDateTime.now());
            passedTests.incrementAndGet();

            log("✓ 测试通过: {} (耗时: {} ms)",
                    testName,
                    Duration.between(startTime, result.getEndTime()).toMillis());

        } catch (AssertionError e) {
            // 测试失败
            result.setStatus(TestStatus.FAILED);
            result.setEndTime(LocalDateTime.now());
            result.setErrorMessage(e.getMessage());
            failedTests.incrementAndGet();

            log("✗ 测试失败: {} (耗时: {} ms)",
                    testName,
                    Duration.between(startTime, result.getEndTime()).toMillis());
            log("  错误: {}", e.getMessage());

        } catch (Throwable e) {
            // 测试错误
            result.setStatus(TestStatus.ERROR);
            result.setEndTime(LocalDateTime.now());
            result.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
            failedTests.incrementAndGet();

            log("✗ 测试错误: {} (耗时: {} ms)",
                    testName,
                    Duration.between(startTime, result.getEndTime()).toMillis());
            log("  错误: {}", result.getErrorMessage());

        } finally {
            testPoints.put(testName, result);
        }
    }

    /**
     * 跳过测试
     */
    public void skipTest(String testName, String reason) {
        totalTests.incrementAndGet();
        skippedTests.incrementAndGet();

        TestPointResult result = new TestPointResult();
        result.setTestName(testName);
        result.setStatus(TestStatus.SKIPPED);
        result.setSkipReason(reason);
        result.setPhase(currentPhase);

        testPoints.put(testName, result);

        log("- 测试跳过: {} - {}", testName, reason);
    }

    /**
     * 添加断言
     */
    public void assertCondition(String testName, String condition, boolean result) {
        if (!result) {
            throw new AssertionError("断言失败: " + condition);
        }
    }

    /**
     * 记录性能指标
     */
    public void recordMetric(String metricName, double value, String unit) {
        PerformanceMetric metric = new PerformanceMetric();
        metric.setMetricName(metricName);
        metric.setValue(value);
        metric.setUnit(unit);
        metric.setRecordedAt(LocalDateTime.now());

        performanceMetrics.put(metricName, metric);

        log("性能指标: {} = {} {}", metricName, value, unit);
    }

    /**
     * 获取通过率
     */
    public double getPassRate() {
        int total = totalTests.get();
        if (total == 0) return 0.0;
        return (double) passedTests.get() / total;
    }

    /**
     * 获取总耗时
     */
    public Duration getDuration() {
        if (suiteStartTime == null) return Duration.ZERO;
        LocalDateTime end = suiteEndTime != null ? suiteEndTime : LocalDateTime.now();
        return Duration.between(suiteStartTime, end);
    }

    /**
     * 生成测试报告
     */
    public TestReport generateReport() {
        TestReport report = new TestReport();
        report.setSuiteName(suiteName);
        report.setStartTime(suiteStartTime);
        report.setEndTime(suiteEndTime);
        report.setDuration(getDuration());
        report.setTotalTests(totalTests.get());
        report.setPassedTests(passedTests.get());
        report.setFailedTests(failedTests.get());
        report.setSkippedTests(skippedTests.get());
        report.setPassRate(getPassRate());

        // 添加测试点结果
        report.setTestPoints(new ArrayList<>(testPoints.values()));

        // 添加性能指标
        report.setPerformanceMetrics(new ArrayList<>(performanceMetrics.values()));

        return report;
    }

    /**
     * 打印测试摘要
     */
    public void printSummary() {
        log("\n=== 测试摘要 ===");
        log("测试套件: {}", suiteName);
        log("总测试数: {}", totalTests.get());
        log("通过: {}", passedTests.get());
        log("失败: {}", failedTests.get());
        log("跳过: {}", skippedTests.get());
        log("通过率: " + String.format("%.1f", getPassRate() * 100) + "%");
        log("总耗时: {} ms", getDuration().toMillis());

        if (failedTests.get() > 0) {
            log("\n失败的测试:");
            for (TestPointResult result : testPoints.values()) {
                if (result.getStatus() == TestStatus.FAILED || result.getStatus() == TestStatus.ERROR) {
                    log("  - {}: {}", result.getTestName(), result.getErrorMessage());
                }
            }
        }
        log("================\n");
    }

    /**
     * 导出JSON报告
     */
    public String exportJsonReport() {
        TestReport report = generateReport();
        // 简化实现，实际应使用Jackson
        return report.toString();
    }

    /**
     * 清理
     */
    public static void cleanup(String suiteName) {
        instances.remove(suiteName);
    }

    /**
     * 清理所有
     */
    public static void cleanupAll() {
        instances.clear();
    }

    /**
     * 日志输出（简化）- 支持{}占位符
     */
    private void log(String message, Object... args) {
        String result = message;
        int argIndex = 0;
        while (result.contains("{}") && argIndex < args.length) {
            result = result.replaceFirst("\\{}", String.valueOf(args[argIndex++]));
        }
        System.out.println(result);
    }

    // ===== 内部类 =====

    /**
     * 测试执行器接口
     */
    @FunctionalInterface
    public interface TestExecutor {
        void execute() throws Throwable;
    }

    /**
     * 测试优先级
     */
    public enum TestPriority {
        CRITICAL, HIGH, NORMAL, LOW
    }

    /**
     * 测试状态
     */
    public enum TestStatus {
        PENDING, RUNNING, PASSED, FAILED, ERROR, SKIPPED
    }

    /**
     * 测试点结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPointResult {
        private String testName;
        private TestStatus status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String errorMessage;
        private String skipReason;
        private TestPriority priority;
        private String phase;
        private Map<String, Object> metadata;

        public Duration getDuration() {
            if (startTime == null || endTime == null) return Duration.ZERO;
            return Duration.between(startTime, endTime);
        }
    }

    /**
     * 性能指标
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetric {
        private String metricName;
        private Double value;
        private String unit;
        private LocalDateTime recordedAt;
    }

    /**
     * 测试报告
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestReport {
        private String suiteName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Duration duration;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int skippedTests;
        private double passRate;
        private List<TestPointResult> testPoints;
        private List<PerformanceMetric> performanceMetrics;
    }
}
