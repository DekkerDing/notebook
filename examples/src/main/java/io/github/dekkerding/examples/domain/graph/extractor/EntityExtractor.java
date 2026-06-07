package io.github.dekkerding.examples.domain.graph.extractor;

import io.github.dekkerding.examples.domain.graph.model.Entity;

import java.util.List;

/**
 * 实体抽取接口
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface EntityExtractor {

    /**
     * 从文本中抽取实体
     *
     * @param text 输入文本
     * @param kbId 知识库ID
     * @return 实体列表
     */
    List<Entity> extract(String text, String kbId);

    /**
     * 获取支持的实体类型
     *
     * @return 实体类型列表
     */
    List<String> getSupportedTypes();
}
