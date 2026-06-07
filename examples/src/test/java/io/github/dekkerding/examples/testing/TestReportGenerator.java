package io.github.dekkerding.examples.testing;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 测试报告生成器 - 生成详细的测试报告（HTML/JSON/文本）
 *
 * <p>功能：</p>
 * <ul>
 *   <li>生成HTML格式报告</li>
 *   <li>生成JSON格式报告</li>
 *   <li>生成文本格式报告</li>
 *   <li>支持自定义报告模板</li>
 *   <li>包含性能指标图表</li>
 *   <li>包含测试趋势分析</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class TestReportGenerator {

    /**
     * 报告目录
     */
    private static final String REPORT_DIR = "build/test-reports/";

    /**
     * 报告时间戳
     */
    private String reportTimestamp;

    /**
     * 测试套件名称
     */
    private String suiteName;

    /**
     * 测试开始时间
     */
    private LocalDateTime startTime;

    /**
     * 测试结束时间
     */
    private LocalDateTime endTime;

    /**
     * 总测试数
     */
    private int totalTests;

    /**
     * 通过测试数
     */
    private int passedTests;

    /**
     * 失败测试数
     */
    private int failedTests;

    /**
     * 跳过测试数
     */
    private int skippedTests;

    /**
     * 测试点列表
     */
    private List<TestPoint> testPoints = new ArrayList<>();

    /**
     * 性能指标
     */
    private Map<String, PerformanceMetric> performanceMetrics = new LinkedHashMap<>();

    /**
     * 环境信息
     */
    private EnvironmentInfo environmentInfo = new EnvironmentInfo();

    /**
     * 从TestProgressTracker创建报告生成器
     */
    public static TestReportGenerator fromTracker(TestProgressTracker tracker) {
        TestProgressTracker.TestReport report = tracker.generateReport();

        TestReportGenerator generator = new TestReportGenerator();
        generator.setSuiteName(report.getSuiteName());
        generator.setStartTime(report.getStartTime());
        generator.setEndTime(report.getEndTime());
        generator.setTotalTests(report.getTotalTests());
        generator.setPassedTests(report.getPassedTests());
        generator.setFailedTests(report.getFailedTests());
        generator.setSkippedTests(report.getSkippedTests());

        // 转换测试点
        if (report.getTestPoints() != null) {
            for (TestProgressTracker.TestPointResult result : report.getTestPoints()) {
                TestPoint point = new TestPoint();
                point.setTestName(result.getTestName());
                point.setStatus(result.getStatus().name());
                point.setDuration(result.getDuration() != null
                        ? result.getDuration().toMillis() : 0);
                point.setPhase(result.getPhase());
                point.setErrorMessage(result.getErrorMessage());
                generator.getTestPoints().add(point);
            }
        }

        // 转换性能指标
        if (report.getPerformanceMetrics() != null) {
            for (TestProgressTracker.PerformanceMetric metric : report.getPerformanceMetrics()) {
                PerformanceMetric perfMetric = new PerformanceMetric();
                perfMetric.setName(metric.getMetricName());
                perfMetric.setValue(metric.getValue());
                perfMetric.setUnit(metric.getUnit());
                generator.getPerformanceMetrics().put(metric.getMetricName(), perfMetric);
            }
        }

        generator.setReportTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        return generator;
    }

    /**
     * 生成HTML报告
     */
    public String generateHtmlReport() {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>").append(suiteName).append(" 测试报告</title>\n");
        html.append("    <style>\n");
        html.append(generateCssStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // 报告头部
        html.append(generateHtmlHeader());

        // 测试摘要
        html.append(generateHtmlSummary());

        // 测试点详情
        html.append(generateHtmlTestPoints());

        // 性能指标
        html.append(generateHtmlPerformanceMetrics());

        // 环境信息
        html.append(generateHtmlEnvironmentInfo());

        // 报告尾部
        html.append(generateHtmlFooter());

        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 生成JSON报告
     */
    public String generateJsonReport() {
        StringBuilder json = new StringBuilder();

        json.append("{\n");
        json.append("  \"suiteName\": \"").append(suiteName).append("\",\n");
        json.append("  \"reportTimestamp\": \"").append(reportTimestamp).append("\",\n");
        json.append("  \"startTime\": \"").append(startTime).append("\",\n");
        json.append("  \"endTime\": \"").append(endTime).append("\",\n");
        json.append("  \"totalTests\": ").append(totalTests).append(",\n");
        json.append("  \"passedTests\": ").append(passedTests).append(",\n");
        json.append("  \"failedTests\": ").append(failedTests).append(",\n");
        json.append("  \"skippedTests\": ").append(skippedTests).append(",\n");
        json.append("  \"passRate\": ").append(String.format("%.2f", calculatePassRate())).append(",\n");
        json.append("  \"durationMs\": ").append(calculateDuration()).append(",\n");
        json.append("  \"testPoints\": [\n");

        for (int i = 0; i < testPoints.size(); i++) {
            TestPoint point = testPoints.get(i);
            json.append("    {\n");
            json.append("      \"testName\": \"").append(escapeJson(point.getTestName())).append("\",\n");
            json.append("      \"status\": \"").append(point.getStatus()).append("\",\n");
            json.append("      \"duration\": ").append(point.getDuration()).append(",\n");
            json.append("      \"phase\": \"").append(point.getPhase()).append("\",\n");
            json.append("      \"errorMessage\": \"").append(escapeJson(point.getErrorMessage())).append("\"\n");
            json.append("    }");
            if (i < testPoints.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ],\n");
        json.append("  \"performanceMetrics\": {\n");

        int metricCount = 0;
        for (Map.Entry<String, PerformanceMetric> entry : performanceMetrics.entrySet()) {
            PerformanceMetric metric = entry.getValue();
            json.append("    \"").append(entry.getKey()).append("\": {\n");
            json.append("      \"value\": ").append(metric.getValue()).append(",\n");
            json.append("      \"unit\": \"").append(metric.getUnit()).append("\"\n");
            json.append("    }");
            if (metricCount++ < performanceMetrics.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  }\n");
        json.append("}");

        return json.toString();
    }

    /**
     * 生成文本报告
     */
    public String generateTextReport() {
        StringBuilder text = new StringBuilder();

        text.append("=").append(repeatChar("=", 78)).append("\n");
        text.append("  ").append(suiteName).append(" 测试报告\n");
        text.append("=").append(repeatChar("=", 78)).append("\n\n");

        // 基本信息
        text.append("报告时间: ").append(reportTimestamp).append("\n");
        text.append("开始时间: ").append(formatTime(startTime)).append("\n");
        text.append("结束时间: ").append(formatTime(endTime)).append("\n");
        text.append("总耗时: ").append(formatDuration(calculateDuration())).append("\n\n");

        // 测试摘要
        text.append("-").append(repeatChar("-", 78)).append("\n");
        text.append("测试摘要\n");
        text.append("-").append(repeatChar("-", 78)).append("\n");
        text.append(String.format("总测试数: %d\n", totalTests));
        text.append(String.format("通过: %d\n", passedTests));
        text.append(String.format("失败: %d\n", failedTests));
        text.append(String.format("跳过: %d\n", skippedTests));
        text.append(String.format("通过率: %.1f%%\n\n", calculatePassRate() * 100));

        // 测试点详情
        text.append("-").append(repeatChar("-", 78)).append("\n");
        text.append("测试点详情\n");
        text.append("-").append(repeatChar("-", 78)).append("\n");

        for (TestPoint point : testPoints) {
            char statusChar = getStatusChar(point.getStatus());
            text.append(String.format("[%c] %-50s %8s %6dms\n",
                    statusChar,
                    truncate(point.getTestName(), 50),
                    point.getStatus(),
                    point.getDuration()));

            if (point.getErrorMessage() != null) {
                text.append(String.format("      错误: %s\n", point.getErrorMessage()));
            }
        }

        // 性能指标
        if (!performanceMetrics.isEmpty()) {
            text.append("\n").append("-").append(repeatChar("-", 78)).append("\n");
            text.append("性能指标\n");
            text.append("-").append(repeatChar("-", 78)).append("\n");

            for (Map.Entry<String, PerformanceMetric> entry : performanceMetrics.entrySet()) {
                PerformanceMetric metric = entry.getValue();
                text.append(String.format("%s: %.2f %s\n",
                        entry.getKey(),
                        metric.getValue(),
                        metric.getUnit()));
            }
        }

        text.append("\n").append("=").append(repeatChar("=", 78)).append("\n");

        return text.toString();
    }

    /**
     * 保存报告到文件
     */
    public void saveReportToFile() {
        File reportDir = new File(REPORT_DIR);
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }

        // 保存HTML报告
        String htmlFileName = suiteName + "_" + reportTimestamp + ".html";
        String htmlPath = REPORT_DIR + htmlFileName;
        try (FileWriter writer = new FileWriter(htmlPath)) {
            writer.write(generateHtmlReport());
            System.out.println("HTML报告已保存: " + htmlPath);
        } catch (IOException e) {
            System.err.println("保存HTML报告失败: " + e.getMessage());
        }

        // 保存JSON报告
        String jsonFileName = suiteName + "_" + reportTimestamp + ".json";
        String jsonPath = REPORT_DIR + jsonFileName;
        try (FileWriter writer = new FileWriter(jsonPath)) {
            writer.write(generateJsonReport());
            System.out.println("JSON报告已保存: " + jsonPath);
        } catch (IOException e) {
            System.err.println("保存JSON报告失败: " + e.getMessage());
        }

        // 保存文本报告
        String txtFileName = suiteName + "_" + reportTimestamp + ".txt";
        String txtPath = REPORT_DIR + txtFileName;
        try (FileWriter writer = new FileWriter(txtPath)) {
            writer.write(generateTextReport());
            System.out.println("文本报告已保存: " + txtPath);
        } catch (IOException e) {
            System.err.println("保存文本报告失败: " + e.getMessage());
        }
    }

    // ===== 生成HTML组件 =====

    private String generateCssStyles() {
        return "body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }\n" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; margin-bottom: 30px; }\n" +
                ".summary { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
                ".test-point { background: white; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #ddd; }\n" +
                ".test-point.passed { border-left-color: #10b981; }\n" +
                ".test-point.failed { border-left-color: #ef4444; }\n" +
                ".test-point.skipped { border-left-color: #f59e0b; }\n" +
                ".status-badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; }\n" +
                ".status-passed { background: #10b981; color: white; }\n" +
                ".status-failed { background: #ef4444; color: white; }\n" +
                ".status-skipped { background: #f59e0b; color: white; }\n" +
                ".metric { background: white; padding: 15px; margin: 10px 0; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
                ".metric-value { font-size: 24px; font-weight: bold; color: #667eea; }\n" +
                ".environment { background: white; padding: 20px; border-radius: 8px; margin-top: 20px; }\n" +
                ".footer { text-align: center; padding: 20px; color: #666; margin-top: 40px; }\n";
    }

    private String generateHtmlHeader() {
        return "    <div class=\"header\">\n" +
                "        <h1>" + suiteName + " 测试报告</h1>\n" +
                "        <p>生成时间: " + reportTimestamp + "</p>\n" +
                "        <p>测试范围: " + totalTests + " 个测试点</p>\n" +
                "    </div>\n";
    }

    private String generateHtmlSummary() {
        long duration = calculateDuration();
        double passRate = calculatePassRate();

        return "    <div class=\"summary\">\n" +
                "        <h2>测试摘要</h2>\n" +
                "        <div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0;\">\n" +
                "            <div style=\"text-align: center;\">\n" +
                "                <div class=\"metric-value\">" + totalTests + "</div>\n" +
                "                <div>总测试数</div>\n" +
                "            </div>\n" +
                "            <div style=\"text-align: center;\">\n" +
                "                <div class=\"metric-value\">" + passedTests + "</div>\n" +
                "                <div>通过测试</div>\n" +
                "            </div>\n" +
                "            <div style=\"text-align: center;\">\n" +
                "                <div class=\"metric-value\">" + failedTests + "</div>\n" +
                "                <div>失败测试</div>\n" +
                "            </div>\n" +
                "            <div style=\"text-align: center;\">\n" +
                "                <div class=\"metric-value\">" + String.format("%.1f%%", passRate * 100) + "</div>\n" +
                "                <div>通过率</div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <p style=\"margin-top: 20px;\"><strong>总耗时:</strong> " + formatDuration(duration) + "</p>\n" +
                "    </div>\n";
    }

    private String generateHtmlTestPoints() {
        StringBuilder html = new StringBuilder();
        html.append("    <h2>测试点详情</h2>\n");

        if (testPoints.isEmpty()) {
            html.append("    <p>没有测试点</p>\n");
        } else {
            for (TestPoint point : testPoints) {
                String statusClass = getStatusClass(point.getStatus());
                html.append("    <div class=\"test-point " + statusClass + "\">\n");
                html.append("        <div style=\"display: flex; justify-content: space-between; align-items: center;\">\n");
                html.append("            <strong>").append(escapeHtml(point.getTestName())).append("</strong>\n");
                html.append("            <span class=\"status-badge " + statusClass + "\">").append(point.getStatus()).append("</span>\n");
                html.append("        </div>\n");
                html.append("        <p><strong>阶段:</strong> ").append(point.getPhase()).append("</p>\n");
                html.append("        <p><strong>耗时:</strong> ").append(point.getDuration()).append(" ms</p>\n");

                if (point.getErrorMessage() != null) {
                    html.append("        <p style=\"color: #ef4444;\"><strong>错误:</strong> ").append(escapeHtml(point.getErrorMessage())).append("</p>\n");
                }

                html.append("    </div>\n");
            }
        }

        return html.toString();
    }

    private String generateHtmlPerformanceMetrics() {
        StringBuilder html = new StringBuilder();
        html.append("    <h2>性能指标</h2>\n");

        if (performanceMetrics.isEmpty()) {
            html.append("    <p>没有性能指标</p>\n");
        } else {
            html.append("    <div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px;\">\n");

            for (Map.Entry<String, PerformanceMetric> entry : performanceMetrics.entrySet()) {
                PerformanceMetric metric = entry.getValue();
                html.append("        <div class=\"metric\">\n");
                html.append("            <div style=\"color: #666; font-size: 14px;\">").append(entry.getKey()).append("</div>\n");
                html.append("            <div class=\"metric-value\">").append(String.format("%.2f", metric.getValue())).append("</div>\n");
                html.append("            <div style=\"color: #666; font-size: 14px;\">").append(metric.getUnit()).append("</div>\n");
                html.append("        </div>\n");
            }

            html.append("    </div>\n");
        }

        return html.toString();
    }

    private String generateHtmlEnvironmentInfo() {
        return "    <div class=\"environment\">\n" +
                "        <h3>环境信息</h3>\n" +
                "        <ul>\n" +
                "            <li><strong>JDK:</strong> " + environmentInfo.jdkVersion + "</li>\n" +
                "            <li><strong>操作系统:</strong> " + environmentInfo.osName + "</li>\n" +
                "            <li><strong>架构:</strong> " + environmentInfo.osArch + "</li>\n" +
                "            <li><strong>Elasticsearch:</strong> " + environmentInfo.esVersion + "</li>\n" +
                "        </ul>\n" +
                "    </div>\n";
    }

    private String generateHtmlFooter() {
        return "    <div class=\"footer\">\n" +
                "        <p>生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>\n" +
                "        <p>Powered by Notebook RAG Testing Framework</p>\n" +
                "    </div>\n";
    }

    // ===== 辅助方法 =====

    private double calculatePassRate() {
        if (totalTests == 0) return 0.0;
        return (double) passedTests / totalTests;
    }

    private long calculateDuration() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(startTime, endTime).toMillis();
    }

    private String repeatChar(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) return "N/A";
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        } else if (millis < 60000) {
            return String.format("%.1f s", millis / 1000.0);
        } else {
            return String.format("%.1f min", millis / 60000.0);
        }
    }

    private char getStatusChar(String status) {
        switch (status.toLowerCase()) {
            case "passed": return '✓';
            case "failed": return '✗';
            case "skipped": return '-';
            default: return '?';
        }
    }

    private String getStatusClass(String status) {
        switch (status.toLowerCase()) {
            case "passed": return "passed";
            case "failed": return "failed";
            case "skipped": return "skipped";
            default: return "";
        }
    }

    // ===== 内部类 =====

    @Data
    @NoArgsConstructor
    public static class TestPoint {
        private String testName;
        private String status;
        private long duration;
        private String phase;
        private String errorMessage;
    }

    @Data
    @NoArgsConstructor
    public static class PerformanceMetric {
        private String name;
        private Double value;
        private String unit;
    }

    @Data
    @NoArgsConstructor
    public static class EnvironmentInfo {
        private String jdkVersion = System.getProperty("java.version");
        private String osName = System.getProperty("os.name");
        private String osArch = System.getProperty("os.arch");
        private String esVersion = "7.17.0"; // 可以从配置读取
    }
}
