package io.github.dekkerding.examples.domain.knowledgebase.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 知识库实体 - 聚合根
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "kb_knowledge_base")
public class KbKnowledgeBase {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 64)
    @Builder.Default
    private String icon = "book";

    @Column(length = 512)
    private String categoryPath;

    @Column(length = 16)
    @Builder.Default
    private String status = "active";

    @Column(length = 128)
    private String owner;

    @Column(length = 16)
    @Builder.Default
    private String language = "zh_CN";

    @Column(length = 64)
    private String vectorIndexId;

    @Column
    @Builder.Default
    private int docCount = 0;

    @Column
    @Builder.Default
    private int faqCount = 0;

    @Column
    @Builder.Default
    private long hitCount = 0;

    @Column
    @Builder.Default
    private double satisfaction = 0.0;

    @Column
    @Builder.Default
    private int sortOrder = 0;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== 业务方法 =====

    /**
     * 增加命中次数
     */
    public void incrementHitCount() {
        this.hitCount++;
    }

    /**
     * 更新满意度
     * @param useful 是否有用
     */
    public void updateSatisfaction(boolean useful) {
        // 移动平均: 新满意度 = 旧满意度 * 0.9 + (useful ? 5 : 0) * 0.1
        this.satisfaction = this.satisfaction * 0.9 + (useful ? 5.0 : 0.0) * 0.1;
    }

    /**
     * 增加文档数量
     */
    public void incrementDocCount() {
        this.docCount++;
    }

    /**
     * 减少文档数量
     */
    public void decrementDocCount() {
        if (this.docCount > 0) {
            this.docCount--;
        }
    }

    /**
     * 增加FAQ数量
     */
    public void incrementFaqCount() {
        this.faqCount++;
    }

    /**
     * 减少FAQ数量
     */
    public void decrementFaqCount() {
        if (this.faqCount > 0) {
            this.faqCount--;
        }
    }

    /**
     * 激活知识库
     */
    public void activate() {
        this.status = "active";
    }

    /**
     * 归档知识库
     */
    public void archive() {
        this.status = "archived";
    }

    /**
     * 禁用知识库
     */
    public void disable() {
        this.status = "disabled";
    }

    /**
     * 判断是否激活状态
     */
    public boolean isActive() {
        return "active".equals(this.status);
    }
}
