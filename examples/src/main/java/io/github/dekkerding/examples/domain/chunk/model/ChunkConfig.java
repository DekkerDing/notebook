package io.github.dekkerding.examples.domain.chunk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分块配置 - 控制分块行为的参数
 *
 * <p>功能：</p>
 * <ul>
 *   <li>配置分块大小和重叠</li>
 *   <li>控制分块质量要求</li>
 *   <li>设置分块策略特定参数</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkConfig {

    /**
     * 目标分块大小（字符数）
     */
    @Builder.Default
    private int chunkSize = 500;

    /**
     * 分块重叠大小（字符数）
     * <p>重叠可以保持上下文连贯性，避免重要信息被截断</p>
     */
    @Builder.Default
    private int chunkOverlap = 50;

    /**
     * 硬限制：最大分块大小（字符数）
     * <p>即使没有合适的分割点，也要在此处强制分割</p>
     */
    @Builder.Default
    private int hardMaxSize = 1000;

    /**
     * 软限制：最小分块大小（字符数）
     * <p>小于此大小的片段将尝试与相邻片段合并</p>
     */
    @Builder.Default
    private int softMinSize = 20;

    /**
     * 是否保留分隔符
     * <p>true: 分隔符保留在分块末尾</p>
     * <p>false: 分隔符被移除</p>
     */
    @Builder.Default
    private boolean keepSeparator = false;

    /**
     * 分块后是否进行清理
     * <p>包括：去除空白、规范化空格等</p>
     */
    @Builder.Default
    private boolean cleanup = true;

    /**
     * 是否移除过短的分块
     */
    @Builder.Default
    private boolean removeTooShort = true;

    /**
     * 是否启用语义分块评估
     */
    @Builder.Default
    private boolean enableSemanticEvaluation = false;

    /**
     * 语义相似度阈值（用于语义分块）
     * <p>0.0-1.0，值越小越严格</p>
     */
    @Builder.Default
    private double similarityThreshold = 0.5;

    /**
     * 是否保留元数据
     */
    @Builder.Default
    private boolean preserveMetadata = true;

    /**
     * 自定义分隔符（用于分隔符策略）
     */
    private String[] customSeparators;

    /**
     * 是否启用递归分块
     */
    @Builder.Default
    private boolean enableRecursive = false;

    /**
     * 递归分块的分隔符优先级列表
     * <p>从高到低优先级，如：["\n\n", "\n", " ", ""]</p>
     */
    @Builder.Default
    private String[] recursiveSeparators = new String[]{"\n\n", "\n", " ", ""};

    // ===== 预设配置 =====

    /**
     * 获取默认配置
     */
    public static ChunkConfig defaultConfig() {
        return ChunkConfig.builder()
                .chunkSize(500)
                .chunkOverlap(50)
                .hardMaxSize(1000)
                .softMinSize(20)
                .build();
    }

    /**
     * 获取小文档配置
     * <p>适用于较短的文档，产生较小的分块</p>
     */
    public static ChunkConfig smallDocumentConfig() {
        return ChunkConfig.builder()
                .chunkSize(200)
                .chunkOverlap(20)
                .hardMaxSize(400)
                .softMinSize(10)
                .build();
    }

    /**
     * 获取大文档配置
     * <p>适用于较长的文档，产生较大的分块</p>
     */
    public static ChunkConfig largeDocumentConfig() {
        return ChunkConfig.builder()
                .chunkSize(1000)
                .chunkOverlap(100)
                .hardMaxSize(2000)
                .softMinSize(50)
                .build();
    }

    /**
     * 获取代码文档配置
     * <p>适用于代码文档，保持代码完整性</p>
     */
    public static ChunkConfig codeDocumentConfig() {
        return ChunkConfig.builder()
                .chunkSize(800)
                .chunkOverlap(80)
                .hardMaxSize(1500)
                .softMinSize(30)
                .keepSeparator(true)
                .customSeparators(new String[]{"\n\n", "\n", ";", "{", "}"})
                .build();
    }

    /**
     * 获取表格配置
     * <p>适用于表格数据，按行分割</p>
     */
    public static ChunkConfig tableConfig() {
        return ChunkConfig.builder()
                .chunkSize(1000)
                .chunkOverlap(0)
                .hardMaxSize(2000)
                .softMinSize(5)
                .keepSeparator(false)
                .customSeparators(new String[]{"\n"})
                .build();
    }

    // ===== 验证方法 =====

    /**
     * 验证配置有效性
     */
    public boolean isValid() {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        if (chunkOverlap < 0) {
            throw new IllegalArgumentException("chunkOverlap cannot be negative");
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be less than chunkSize");
        }
        if (hardMaxSize < chunkSize) {
            throw new IllegalArgumentException("hardMaxSize must be >= chunkSize");
        }
        if (softMinSize >= chunkSize) {
            throw new IllegalArgumentException("softMinSize must be < chunkSize");
        }
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("similarityThreshold must be between 0.0 and 1.0");
        }
        return true;
    }

    /**
     * 计算实际分块大小
     * <p>考虑重叠后的实际大小</p>
     */
    public int getEffectiveChunkSize() {
        return chunkSize + chunkOverlap;
    }

    /**
     * 检查是否应该合并分块
     * @param chunkLength 分块长度
     * @return true if chunk should be merged
     */
    public boolean shouldMerge(int chunkLength) {
        return chunkLength > 0 && chunkLength < softMinSize;
    }

    /**
     * 检查分块是否过大
     * @param chunkLength 分块长度
     * @return true if chunk is too large
     */
    public boolean isTooLarge(int chunkLength) {
        return chunkLength > hardMaxSize;
    }

    /**
     * 获取分隔符列表
     */
    public String[] getSeparators() {
        if (customSeparators != null && customSeparators.length > 0) {
            return customSeparators;
        }
        return recursiveSeparators;
    }

    /**
     * 克隆配置
     */
    public ChunkConfig copy() {
        return ChunkConfig.builder()
                .chunkSize(this.chunkSize)
                .chunkOverlap(this.chunkOverlap)
                .hardMaxSize(this.hardMaxSize)
                .softMinSize(this.softMinSize)
                .keepSeparator(this.keepSeparator)
                .cleanup(this.cleanup)
                .removeTooShort(this.removeTooShort)
                .enableSemanticEvaluation(this.enableSemanticEvaluation)
                .similarityThreshold(this.similarityThreshold)
                .preserveMetadata(this.preserveMetadata)
                .customSeparators(this.customSeparators != null
                        ? this.customSeparators.clone() : null)
                .enableRecursive(this.enableRecursive)
                .recursiveSeparators(this.recursiveSeparators != null
                        ? this.recursiveSeparators.clone() : null)
                .build();
    }

    @Override
    public String toString() {
        return "ChunkConfig{" +
                "chunkSize=" + chunkSize +
                ", chunkOverlap=" + chunkOverlap +
                ", hardMaxSize=" + hardMaxSize +
                ", softMinSize=" + softMinSize +
                ", keepSeparator=" + keepSeparator +
                ", cleanup=" + cleanup +
                '}';
    }
}
