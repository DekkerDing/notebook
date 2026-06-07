package io.github.dekkerding.examples.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果
 *
 * @param <T> 数据类型
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResult<T> {

    /**
     * 数据列表
     */
    private List<T> items;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码（从0开始）
     */
    @Builder.Default
    private int page = 0;

    /**
     * 每页大小
     */
    @Builder.Default
    private int size = 20;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 是否有上一页
     */
    private boolean hasPrevious;

    /**
     * 从Spring Data Page创建
     */
    public static <T> PageResult<T> of(org.springframework.data.domain.Page<T> page) {
        return PageResult.<T>builder()
                .items(page.getContent())
                .total(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * 从列表创建
     */
    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        int totalPages = (int) Math.ceil((double) total / size);

        return PageResult.<T>builder()
                .items(items)
                .total(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }

    /**
     * 空结果
     */
    public static <T> PageResult<T> empty() {
        return PageResult.<T>builder()
                .items(java.util.Collections.emptyList())
                .total(0)
                .page(0)
                .size(0)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    /**
     * 获取第一条数据
     */
    public T getFirst() {
        return (items != null && !items.isEmpty()) ? items.get(0) : null;
    }

    /**
     * 获取最后一条数据
     */
    public T getLast() {
        return (items != null && !items.isEmpty()) ? items.get(items.size() - 1) : null;
    }
}
