package io.github.dekkerding.examples.domain.chunk.strategy.impl;

import io.github.dekkerding.examples.domain.chunk.model.ChunkConfig;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode.ChunkMetadata;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode.ChunkType;
import io.github.dekkerding.examples.domain.chunk.strategy.ChunkStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 表格分块策略 - 专门处理表格数据的分块策略
 *
 * <p>表格分块策略用于处理Markdown、HTML、Excel等格式的表格数据。</p>
 *
 * <p>核心特性：</p>
 * <ul>
 *   <li>保持表格结构完整性</li>
 *   <li>按行分割表格</li>
 *   <li>保留表头信息</li>
 *   <li>支持表格元数据（列名、类型等）</li>
 * </ul>
 *
 * <p>支持的表格格式：</p>
 * <ul>
 *   <li>Markdown表格</li>
 *   <li>HTML表格</li>
 *   <li>CSV格式</li>
 *   <li>Excel表格</li>
 * </ul>
 *
 * <p>工作流程：</p>
 * <pre>
 * 1. 识别表格格式
 * 2. 解析表格结构（表头、行、列）
 * 3. 计算合适的行数分块
 * 4. 创建包含表头的分块
 * 5. 添加表格元数据
 * </pre>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class TableChunkStrategy implements ChunkStrategy {

    /**
     * Markdown表格分隔符
     */
    private static final String MD_TABLE_ROW_DELIMITER = "\\|";

    /**
     * HTML表格标签
     */
    private static final String[] HTML_TABLE_TAGS = {"<table", "<tr", "<td", "<th"};

    @Override
    public List<ChunkNode> split(String text, ChunkConfig config) {
        log.debug("开始表格分块: textLength={}, config={}",
                text != null ? text.length() : 0, config);

        // 验证输入
        if (!isValidInput(text)) {
            return Collections.emptyList();
        }

        // 检测表格类型
        TableType tableType = detectTableType(text);

        switch (tableType) {
            case MARKDOWN:
                return splitMarkdownTable(text, config);
            case HTML:
                return splitHtmlTable(text, config);
            case CSV:
                return splitCsvTable(text, config);
            default:
                log.warn("未识别的表格格式，使用默认分块");
                return Collections.singletonList(createChunkNode(text, 0, config));
        }
    }

    @Override
    public String getStrategyName() {
        return "table";
    }

    @Override
    public String getDescription() {
        return "专门处理表格数据的分块策略，支持Markdown、HTML、CSV等格式";
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
                "text/markdown",
                "text/html",
                "text/csv",
                "application/vnd.ms-excel"
        };
    }

    /**
     * 检测表格类型
     */
    private TableType detectTableType(String text) {
        // 检测Markdown表格
        if (text.contains("|") && text.contains("---")) {
            return TableType.MARKDOWN;
        }

        // 检测HTML表格
        for (String tag : HTML_TABLE_TAGS) {
            if (text.contains(tag)) {
                return TableType.HTML;
            }
        }

        // 检测CSV
        if (text.contains(",") && text.split("\n").length > 1) {
            return TableType.CSV;
        }

        return TableType.UNKNOWN;
    }

    /**
     * 分割Markdown表格
     */
    private List<ChunkNode> splitMarkdownTable(String text, ChunkConfig config) {
        List<String> lines = Arrays.asList(text.split("\n"));

        // 查找表头
        int headerLineIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("|")) {
                headerLineIndex = i;
                break;
            }
        }

        if (headerLineIndex == -1) {
            return Collections.singletonList(createChunkNode(text, 0, config));
        }

        // 解析表头
        String header = lines.get(headerLineIndex);
        String[] headers = parseMarkdownRow(header);

        // 解析数据行
        List<String> dataRows = new ArrayList<>();
        for (int i = headerLineIndex + 2; i < lines.size(); i++) { // +2 跳过分隔行
            String line = lines.get(i);
            if (line.trim().isEmpty()) continue;
            if (!line.contains("|")) continue;
            dataRows.add(line);
        }

        if (dataRows.isEmpty()) {
            return Collections.singletonList(createChunkNode(text, 0, config));
        }

        // 计算每块行数
        int chunkSize = config.getChunkSize();
        int rowsPerChunk = Math.max(1, chunkSize / (header.length() + 50));

        // 创建分块
        List<ChunkNode> chunks = new ArrayList<>();

        for (int i = 0; i < dataRows.size(); i += rowsPerChunk) {
            int end = Math.min(i + rowsPerChunk, dataRows.size());
            List<String> chunkRows = dataRows.subList(i, end);

            // 构建表格文本
            StringBuilder tableBuilder = new StringBuilder();
            tableBuilder.append(header).append("\n");
            tableBuilder.append(createMarkdownSeparator(headers.length)).append("\n");

            for (String row : chunkRows) {
                tableBuilder.append(row).append("\n");
            }

            ChunkNode chunk = createChunkNode(
                    tableBuilder.toString(),
                    chunks.size(),
                    config
            );

            // 添加表格元数据
            addTableMetadata(chunk, headers, i, end);

            chunks.add(chunk);
        }

        log.debug("Markdown表格分块完成: totalRows={}, chunks={}",
                dataRows.size(), chunks.size());

        return chunks;
    }

    /**
     * 解析Markdown行
     */
    private String[] parseMarkdownRow(String row) {
        String trimmed = row.trim();
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.split("\\|");
    }

    /**
     * 创建Markdown分隔符行
     */
    private String createMarkdownSeparator(int columnCount) {
        String[] separators = new String[columnCount];
        Arrays.fill(separators, "---");
        return "|" + String.join("|", separators) + "|";
    }

    /**
     * 分割HTML表格
     */
    private List<ChunkNode> splitHtmlTable(String text, ChunkConfig config) {
        // 简化实现：按<tr>标签分割
        List<String> rows = new ArrayList<>();

        String[] parts = text.split("<tr");
        for (String part : parts) {
            if (part.contains("<td") || part.contains("<th")) {
                rows.add("<tr" + part);
            }
        }

        if (rows.isEmpty()) {
            return Collections.singletonList(createChunkNode(text, 0, config));
        }

        // 查找表头
        String headerRow = null;
        List<String> dataRows = new ArrayList<>();

        for (String row : rows) {
            if (row.contains("<th")) {
                headerRow = row;
            } else if (row.contains("<td")) {
                dataRows.add(row);
            }
        }

        // 计算分块
        int chunkSize = config.getChunkSize();
        int rowsPerChunk = Math.max(1, chunkSize / 200);

        List<ChunkNode> chunks = new ArrayList<>();

        for (int i = 0; i < dataRows.size(); i += rowsPerChunk) {
            int end = Math.min(i + rowsPerChunk, dataRows.size());

            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<table>\n");

            if (headerRow != null) {
                htmlBuilder.append(headerRow).append("\n");
            }

            for (int j = i; j < end; j++) {
                htmlBuilder.append(dataRows.get(j)).append("\n");
            }

            htmlBuilder.append("</table>");

            ChunkNode chunk = createChunkNode(
                    htmlBuilder.toString(),
                    chunks.size(),
                    config
            );

            chunks.add(chunk);
        }

        log.debug("HTML表格分块完成: totalRows={}, chunks={}",
                dataRows.size(), chunks.size());

        return chunks;
    }

    /**
     * 分割CSV表格
     */
    private List<ChunkNode> splitCsvTable(String text, ChunkConfig config) {
        List<String> lines = Arrays.asList(text.split("\n"));

        if (lines.isEmpty()) {
            return Collections.emptyList();
        }

        // 第一行作为表头
        String headerLine = lines.get(0);
        String[] headers = headerLine.split(",");

        // 数据行
        List<String> dataRows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                dataRows.add(line);
            }
        }

        if (dataRows.isEmpty()) {
            return Collections.singletonList(createChunkNode(text, 0, config));
        }

        // 计算分块
        int chunkSize = config.getChunkSize();
        int rowsPerChunk = Math.max(1, chunkSize / (headerLine.length() + 50));

        List<ChunkNode> chunks = new ArrayList<>();

        for (int i = 0; i < dataRows.size(); i += rowsPerChunk) {
            int end = Math.min(i + rowsPerChunk, dataRows.size());

            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append(headerLine).append("\n");

            for (int j = i; j < end; j++) {
                csvBuilder.append(dataRows.get(j)).append("\n");
            }

            ChunkNode chunk = createChunkNode(
                    csvBuilder.toString(),
                    chunks.size(),
                    config
            );

            // 添加表格元数据
            addTableMetadata(chunk, headers, i, end);

            chunks.add(chunk);
        }

        log.debug("CSV表格分块完成: totalRows={}, chunks={}",
                dataRows.size(), chunks.size());

        return chunks;
    }

    /**
     * 创建分块节点
     */
    private ChunkNode createChunkNode(String content, int position, ChunkConfig config) {
        return ChunkNode.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .position(position)
                .metadata(ChunkMetadata.builder()
                        .chunkType(ChunkType.TABLE)
                        .charCount(content.length())
                        .strategyName(getStrategyName())
                        .build())
                .build();
    }

    /**
     * 添加表格元数据
     */
    private void addTableMetadata(ChunkNode chunk, String[] headers, int startRow, int endRow) {
        ChunkMetadata metadata = chunk.getMetadata();

        // 添加表头信息
        metadata.getExtra().put("table_headers", Arrays.asList(headers));
        metadata.getExtra().put("table_row_count", endRow - startRow);
        metadata.getExtra().put("table_start_row", startRow);
        metadata.getExtra().put("table_end_row", endRow);
        metadata.getExtra().put("table_column_count", headers.length);
    }

    /**
     * 表格类型枚举
     */
    private enum TableType {
        MARKDOWN,
        HTML,
        CSV,
        UNKNOWN
    }
}
