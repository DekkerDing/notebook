package io.github.dekkerding.examples.domain.retrieval.fusion;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 策略执行结果
 *
 * <p>记录单个检索策略的执行结果和元数据</p>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyResult {

    /**
     * 策略名称
     */
    private String strategyName;

    /**
     * 检索结果列表
     */
    private java.util.List<RetrieveResult> results;

    /**
     * 执行延迟（毫秒）
     */
    private long latency;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;
}
