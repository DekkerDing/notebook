package io.github.dekkerding.examples.interfaces.retrieval.dto;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索响应
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    /**
     * 查询文本
     */
    private String query;

    /**
     * 搜索结果
     */
    private List<RetrieveResultDto> results;

    /**
     * 结果总数
     */
    private int total;

    /**
     * 检索耗时（毫秒）
     */
    private long duration;

    /**
     * 统计信息
     */
    private RetrieveStatisticsDto statistics;

    /**
     * 是否有结果
     */
    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    /**
     * 获取Top3结果
     */
    public List<RetrieveResultDto> getTopResults() {
        if (results == null || results.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        int end = Math.min(3, results.size());
        return results.subList(0, end);
    }
}
