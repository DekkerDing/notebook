package io.github.dekkerding.examples.domain.chunk.strategy;

import io.github.dekkerding.examples.domain.chunk.model.ChunkConfig;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode;

import java.util.List;

/**
 * 分块策略接口 - 所有分块策略的基础接口
 *
 * <p>功能：</p>
 * <ul>
 *   <li>定义分块行为规范</li>
 *   <li>支持不同文档类型的分块策略</li>
 *   <li>提供策略元信息</li>
 * </ul>
 *
 * <p>实现类：</p>
 * <ul>
 *   <li>SeparatorChunkStrategy - 分隔符分块</li>
 *   <li>RecursiveChunkStrategy - 递归字符分块</li>
 *   <li>SemanticChunkStrategy - 语义分块</li>
 *   <li>TableChunkStrategy - 表格分块</li>
 *   <li>HierarchyChunkStrategy - 层次标题分块</li>
 *   <li>CodeChunkStrategy - 代码分块</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface ChunkStrategy {

    /**
     * 执行分块
     *
     * @param text  待分块的文本
     * @param config 分块配置
     * @return 分块节点列表
     * @throws IllegalArgumentException 如果参数无效
     */
    List<ChunkNode> split(String text, ChunkConfig config);

    /**
     * 获取策略名称
     * @return 策略名称（如 "separator", "recursive", "semantic" 等）
     */
    String getStrategyName();

    /**
     * 获取策略描述
     * @return 策略描述
     */
    default String getDescription() {
        return "Default chunk strategy";
    }

    /**
     * 获取策略支持的文档类型
     * @return 支持的文档类型列表（如 "text/plain", "text/html", "application/pdf" 等）
     */
    default String[] getSupportedTypes() {
        return new String[]{"text/plain"};
    }

    /**
     * 验证输入文本
     * @param text 输入文本
     * @return true if text is valid for chunking
     */
    default boolean isValidInput(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * 验证配置
     * @param config 分块配置
     * @return true if config is valid for this strategy
     */
    default boolean isValidConfig(ChunkConfig config) {
        return config != null && config.isValid();
    }

    /**
     * 预处理文本
     * <p>在分块前对文本进行预处理，如去除多余空白、规范化等</p>
     *
     * @param text 原始文本
     * @param config 分块配置
     * @return 处理后的文本
     */
    default String preprocessText(String text, ChunkConfig config) {
        if (!config.isCleanup()) {
            return text;
        }

        // 去除首尾空白
        String processed = text.trim();

        // 规范化连续空白（保留单个空格）
        processed = processed.replaceAll("\\s+", " ");

        return processed;
    }

    /**
     * 后处理分块列表
     * <p>在分块后对结果进行后处理，如合并过短分块、过滤空白等</p>
     *
     * @param chunks 原始分块列表
     * @param config 分块配置
     * @return 处理后的分块列表
     */
    default List<ChunkNode> postProcessChunks(List<ChunkNode> chunks, ChunkConfig config) {
        if (chunks == null || chunks.isEmpty()) {
            return chunks;
        }

        // 过滤无效分块
        chunks.removeIf(chunk -> !chunk.isValid());

        // 可选：移除过短分块
        if (config.isRemoveTooShort()) {
            chunks.removeIf(chunk -> chunk.getLength() < config.getSoftMinSize());
        }

        // 更新位置信息
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setPosition(i);
        }

        return chunks;
    }

    /**
     * 计算分块质量分数
     * <p>分数越高表示分块质量越好</p>
     *
     * @param chunks 分块列表
     * @param config 分块配置
     * @return 质量分数（0.0-1.0）
     */
    default double calculateQualityScore(List<ChunkNode> chunks, ChunkConfig config) {
        if (chunks == null || chunks.isEmpty()) {
            return 0.0;
        }

        int totalChunks = chunks.size();
        int idealChunks = (int) Math.ceil(config.getChunkSize() / (double) config.getChunkSize());

        // 计算大小分布
        double avgSize = chunks.stream()
                .mapToInt(ChunkNode::getLength)
                .average()
                .orElse(0.0);

        // 计算标准差
        double variance = chunks.stream()
                .mapToDouble(chunk -> Math.pow(chunk.getLength() - avgSize, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double cv = avgSize > 0 ? stdDev / avgSize : 0; // 变异系数

        // 质量分数：考虑数量接近度和大小一致性
        double countScore = 1.0 - Math.abs(totalChunks - idealChunks) / (double) Math.max(totalChunks, idealChunks);
        double consistencyScore = 1.0 - Math.min(cv, 1.0);

        return (countScore * 0.4 + consistencyScore * 0.6);
    }
}
