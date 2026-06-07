package io.github.dekkerding.examples.domain.chunk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 分块节点 - 表示文档被切分后的一个文本块
 *
 * <p>功能：</p>
 * <ul>
 *   <li>存储分块内容和元数据</li>
 *   <li>支持分块层级结构（父子关系）</li>
 *   <li>记录分块来源位置</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>文档分块后的每个片段</li>
 *   <li>表格单元格内容</li>
 *   <li>代码片段</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkNode {

    /**
     * 分块唯一标识
     */
    private String id;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 分块元数据
     */
    @Builder.Default
    private ChunkMetadata metadata = new ChunkMetadata();

    /**
     * 分块在文档中的位置（从0开始）
     */
    private int position;

    /**
     * 父分块ID（用于层级结构，如表格的单元格属于表格）
     */
    private String parentId;

    /**
     * 子分块列表（延迟加载）
     */
    @JsonIgnore
    private java.util.List<ChunkNode> children;

    /**
     * 分块元数据内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkMetadata {
        /**
         * 来源文档ID
         */
        private String sourceDocId;

        /**
         * 分块类型（TEXT/TABLE/CODE/IMAGE等）
         */
        @Builder.Default
        private ChunkType chunkType = ChunkType.TEXT;

        /**
         * 分块策略名称
         */
        private String strategyName;

        /**
         * 字符数
         */
        @Builder.Default
        private int charCount = 0;

        /**
         * 估算token数（中文约1.5字符=1token，英文约4字符=1token）
         */
        @Builder.Default
        private int estimatedTokens = 0;

        /**
         * 起始位置（在原文档中）
         */
        @Builder.Default
        private int startIndex = 0;

        /**
         * 结束位置（在原文档中）
         */
        @Builder.Default
        private int endIndex = 0;

        /**
         * 额外元数据（如表格行列信息、代码语言等）
         */
        @Builder.Default
        private Map<String, Object> extra = new HashMap<>();

        /**
         * 创建时间
         */
        @Builder.Default
        private long createdAt = System.currentTimeMillis();
    }

    /**
     * 分块类型枚举
     */
    public enum ChunkType {
        /**
         * 纯文本
         */
        TEXT,
        /**
         * 表格
         */
        TABLE,
        /**
         * 代码
         */
        CODE,
        /**
         * 图片描述
         */
        IMAGE,
        /**
         * 列表
         */
        LIST,
        /**
         * 标题
         */
        HEADER,
        /**
         * 其他
         */
        OTHER
    }

    // ===== 业务方法 =====

    /**
     * 获取分块内容长度
     */
    public int getLength() {
        return content != null ? content.length() : 0;
    }

    /**
     * 估算token数
     * 中文：约1.5字符=1token
     * 英文：约4字符=1token
     */
    public int estimateTokens() {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        // 检测中文字符比例
        int chineseChars = 0;
        for (char c : content.toCharArray()) {
            if (isChinese(c)) {
                chineseChars++;
            }
        }

        double chineseRatio = (double) chineseChars / content.length();

        // 混合计算
        int chineseTokens = (int) (chineseChars / 1.5);
        int nonChineseTokens = (int) ((content.length() - chineseChars) / 4.0);

        return chineseTokens + nonChineseTokens;
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    /**
     * 创建子分块
     */
    public ChunkNode createChild(String childContent) {
        ChunkNode child = ChunkNode.builder()
                .content(childContent)
                .parentId(this.id)
                .position(this.children != null ? this.children.size() : 0)
                .build();

        child.getMetadata().setSourceDocId(this.getMetadata().getSourceDocId());
        child.getMetadata().setChunkType(this.getMetadata().getChunkType());

        if (this.children == null) {
            this.children = new java.util.ArrayList<>();
        }
        this.children.add(child);

        return child;
    }

    /**
     * 判断是否为根节点（没有父节点）
     */
    public boolean isRoot() {
        return this.parentId == null || this.parentId.isEmpty();
    }

    /**
     * 判断是否为叶子节点（没有子节点）
     */
    public boolean isLeaf() {
        return this.children == null || this.children.isEmpty();
    }

    /**
     * 获取层级深度
     */
    public int getDepth() {
        if (isRoot()) {
            return 0;
        }
        return 1; // 简化实现，实际可以递归计算父节点深度
    }

    /**
     * 合并相邻分块
     */
    public static ChunkNode merge(ChunkNode first, ChunkNode second) {
        if (first == null) return second;
        if (second == null) return first;

        String mergedContent = first.getContent() + "\n\n" + second.getContent();

        ChunkNode merged = ChunkNode.builder()
                .content(mergedContent)
                .position(first.getPosition())
                .build();

        // 合并元数据
        ChunkMetadata mergedMetadata = ChunkMetadata.builder()
                .sourceDocId(first.getMetadata().getSourceDocId())
                .chunkType(first.getMetadata().getChunkType())
                .charCount(first.getMetadata().getCharCount() + second.getMetadata().getCharCount())
                .startIndex(first.getMetadata().getStartIndex())
                .endIndex(second.getMetadata().getEndIndex())
                .strategyName(first.getMetadata().getStrategyName())
                .build();

        merged.setMetadata(mergedMetadata);

        return merged;
    }

    /**
     * 验证分块有效性
     */
    public boolean isValid() {
        return content != null && !content.trim().isEmpty()
                && content.length() > 0;
    }

    /**
     * 获取摘要（前100个字符）
     */
    public String getSummary() {
        if (content == null) return "";
        return content.length() > 100
                ? content.substring(0, 100) + "..."
                : content;
    }

    /**
     * 克隆分块
     */
    public ChunkNode copy() {
        ChunkNode copy = ChunkNode.builder()
                .id(this.id)
                .content(this.content)
                .position(this.position)
                .parentId(this.parentId)
                .build();

        if (this.metadata != null) {
            ChunkMetadata metadataCopy = ChunkMetadata.builder()
                    .sourceDocId(this.metadata.getSourceDocId())
                    .chunkType(this.metadata.getChunkType())
                    .strategyName(this.metadata.getStrategyName())
                    .charCount(this.metadata.getCharCount())
                    .estimatedTokens(this.metadata.getEstimatedTokens())
                    .startIndex(this.metadata.getStartIndex())
                    .endIndex(this.metadata.getEndIndex())
                    .createdAt(this.metadata.getCreatedAt())
                    .build();

            if (this.metadata.getExtra() != null) {
                metadataCopy.setExtra(new HashMap<>(this.metadata.getExtra()));
            }

            copy.setMetadata(metadataCopy);
        }

        return copy;
    }

    @Override
    public String toString() {
        return "ChunkNode{" +
                "id='" + id + '\'' +
                ", length=" + getLength() +
                ", tokens=" + estimateTokens() +
                ", type=" + (metadata != null ? metadata.getChunkType() : "UNKNOWN") +
                ", summary='" + getSummary() + '\'' +
                '}';
    }
}
