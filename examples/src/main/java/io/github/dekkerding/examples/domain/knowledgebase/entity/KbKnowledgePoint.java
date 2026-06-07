package io.github.dekkerding.examples.domain.knowledgebase.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 知识点实体
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "kb_knowledge_point", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"kb_id", "title"})
})
public class KbKnowledgePoint {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "kb_id", nullable = false, length = 64)
    private String kbId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 1000)
    private String summary;

    @Column(length = 500)
    private String keywords;

    @Column(length = 255)
    private String category;

    @Column(length = 500)
    private String tags;

    @Column(length = 255)
    private String source;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(length = 16)
    @Builder.Default
    private String difficulty = "medium";

    @Column(length = 16)
    @Builder.Default
    private String status = "active";

    @Column
    @Builder.Default
    private int priority = 0;

    @Column
    @Builder.Default
    private int viewCount = 0;

    @Column
    @Builder.Default
    private int likeCount = 0;

    @Column
    @Builder.Default
    private int dislikeCount = 0;

    @Column(length = 128)
    private String createdBy;

    @Column(length = 128)
    private String updatedBy;

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
     * 增加查看次数
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 点赞
     */
    public void like() {
        this.likeCount++;
    }

    /**
     * 点踩
     */
    public void dislike() {
        this.dislikeCount++;
    }

    /**
     * 获取点赞率
     */
    public double getLikeRate() {
        int total = likeCount + dislikeCount;
        return total > 0 ? (double) likeCount / total : 0.0;
    }

    /**
     * 激活知识点
     */
    public void activate() {
        this.status = "active";
    }

    /**
     * 归档知识点
     */
    public void archive() {
        this.status = "archived";
    }

    /**
     * 禁用知识点
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

    /**
     * 添加标签
     */
    public void addTag(String tag) {
        if (this.tags == null || this.tags.isEmpty()) {
            this.tags = tag;
        } else {
            this.tags += "," + tag;
        }
    }

    /**
     * 获取标签数组
     */
    public String[] getTagArray() {
        return tags != null && !tags.isEmpty()
                ? tags.split(",")
                : new String[0];
    }

    /**
     * 获取关键词数组
     */
    public String[] getKeywordArray() {
        return keywords != null && !keywords.isEmpty()
                ? keywords.split(",")
                : new String[0];
    }
}
