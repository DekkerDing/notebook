package io.github.dekkerding.examples.domain.document.model;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 解析后的文档统一结果模型
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
public class ParsedDocument {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 提取的文本内容
     */
    private String textContent;

    /**
     * 页面/片段列表
     */
    private java.util.List<Page> pages;

    /**
     * 表格列表
     */
    private java.util.List<Table> tables;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 解析时间戳
     */
    private Date parsedAt;

    /**
     * 页面/片段信息
     */
    @Data
    @Builder
    public static class Page {
        /**
         * 页码
         */
        private Integer pageNumber;

        /**
         * 页面文本内容
         */
        private String content;

        /**
         * 标题层级（用于Word等结构化文档）
         */
        private Integer headingLevel;
    }

    /**
     * 表格信息
     */
    @Data
    @Builder
    public static class Table {
        /**
         * 表格所在页码
         */
        private Integer pageNumber;

        /**
         * 行数据列表
         */
        private java.util.List<java.util.List<String>> rows;

        /**
         * 表头
         */
        private java.util.List<String> headers;
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
}
