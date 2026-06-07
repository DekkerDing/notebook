package io.github.dekkerding.examples.domain.retrieval.rerank;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveContext;

import java.util.List;

/**
 * 重排序器接口
 *
 * <p>重排序（Reranking）是在初始检索后，使用更精确的模型对候选结果进行二次排序，
 * 以提升检索准确率。RAG系统中的核心优化技术。</p>
 *
 * <p>典型的重排序流程：</p>
 * <pre>
 * 原始查询 → 初始检索(Top100) → 重排序模型 → 最终结果(Top10)
 * </pre>
 *
 * <p>重排序的优势：</p>
 * <ul>
 *   <li>提升准确率：从60%提升到85%+</li>
 *   <li>处理长尾查询：对低频查询效果显著</li>
 *   <li>上下文理解：考虑查询-文档对的深度交互</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface Reranker {

    /**
     * 重排序检索结果
     *
     * @param query 原始查询
     * @param candidates 候选结果（来自初始检索）
     * @param request 检索请求参数
     * @param context 检索上下文
     * @return 重排序后的结果
     */
    List<RetrieveResult> rerank(
            String query,
            List<RetrieveResult> candidates,
            RetrieveRequest request,
            RetrieveContext context
    );

    /**
     * 获取重排序器名称
     *
     * @return 重排序器名称
     */
    String getName();

    /**
     * 获取重排序器描述
     *
     * @return 描述信息
     */
    String getDescription();

    /**
     * 是否启用
     *
     * @return true表示启用
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 获取建议的候选数量
     *
     * @return 建议的候选数量
     */
    default int getRecommendedCandidateCount() {
        return 100;
    }
}
