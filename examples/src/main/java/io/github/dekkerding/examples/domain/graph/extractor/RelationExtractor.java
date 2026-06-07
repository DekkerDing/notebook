package io.github.dekkerding.examples.domain.graph.extractor;

import io.github.dekkerding.examples.domain.graph.model.Entity;
import io.github.dekkerding.examples.domain.graph.model.Relation;

import java.util.List;

/**
 * 关系抽取接口
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface RelationExtractor {

    /**
     * 从文本中抽取关系
     *
     * @param text 输入文本
     * @param entities 已识别的实体列表
     * @param kbId 知识库ID
     * @return 关系列表
     */
    List<Relation> extract(String text, List<Entity> entities, String kbId);

    /**
     * 获取支持的关系类型
     *
     * @return 关系类型列表
     */
    List<String> getSupportedTypes();
}
