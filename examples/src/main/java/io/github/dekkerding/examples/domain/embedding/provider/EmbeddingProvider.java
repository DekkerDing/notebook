package io.github.dekkerding.examples.domain.embedding.provider;

import io.github.dekkerding.examples.domain.embedding.model.EmbeddingRequest;
import io.github.dekkerding.examples.domain.embedding.model.EmbeddingResult;

import java.util.List;

/**
 * 向量化提供商接口
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface EmbeddingProvider {

    /**
     * 提供商名称
     */
    String getName();

    /**
     * 向量维度
     */
    int getDimension();

    /**
     * 单个文本向量化
     */
    EmbeddingResult embed(String text);

    /**
     * 单个文本向量化（带参数）
     */
    EmbeddingResult embed(String text, EmbeddingRequest request);

    /**
     * 批量向量化
     */
    List<EmbeddingResult> embedBatch(List<String> texts);

    /**
     * 批量向量化（带参数）
     */
    List<EmbeddingResult> embedBatch(List<String> texts, EmbeddingRequest request);
}
