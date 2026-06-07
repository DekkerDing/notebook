package io.github.dekkerding.examples.interfaces.knowledgebase.dto;

import io.github.dekkerding.examples.domain.knowledgebase.entity.KbKnowledgeBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库响应
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbKnowledgeBaseResponse {

    /**
     * 知识库ID
     */
    private String id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 知识库图标
     */
    private String icon;

    /**
     * 分类路径
     */
    private String categoryPath;

    /**
     * 状态
     */
    private String status;

    /**
     * 所有者
     */
    private String owner;

    /**
     * 语言
     */
    private String language;

    /**
     * 向量索引ID
     */
    private String vectorIndexId;

    /**
     * 文档数量
     */
    private Integer docCount;

    /**
     * FAQ数量
     */
    private Integer faqCount;

    /**
     * 命中次数
     */
    private Long hitCount;

    /**
     * 满意度评分
     */
    private Double satisfaction;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 从实体创建响应
     */
    public static KbKnowledgeBaseResponse fromEntity(KbKnowledgeBase entity) {
        if (entity == null) {
            return null;
        }

        return KbKnowledgeBaseResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .icon(entity.getIcon())
                .categoryPath(entity.getCategoryPath())
                .status(entity.getStatus())
                .owner(entity.getOwner())
                .language(entity.getLanguage())
                .vectorIndexId(entity.getVectorIndexId())
                .docCount(entity.getDocCount())
                .faqCount(entity.getFaqCount())
                .hitCount(entity.getHitCount())
                .satisfaction(entity.getSatisfaction())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 从实体列表创建响应列表
     */
    public static List<KbKnowledgeBaseResponse> fromEntityList(List<KbKnowledgeBase> entities) {
        if (entities == null || entities.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return entities.stream()
                .map(KbKnowledgeBaseResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 判断是否激活
     */
    public boolean isActive() {
        return "active".equals(this.status);
    }

    /**
     * 获取满意度百分比
     */
    public int getSatisfactionPercent() {
        return satisfaction != null ? (int) (satisfaction * 20) : 0; // 5分制转百分比
    }
}
