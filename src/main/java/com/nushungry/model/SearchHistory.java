package com.nushungry.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 搜索历史实体
 * 记录用户的搜索行为,用于搜索建议和数据分析
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "search_history",
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_keyword", columnList = "keyword")
    }
)
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID (可为空,支持未登录用户搜索)
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 搜索关键词
     */
    @Column(nullable = false, length = 200)
    private String keyword;

    /**
     * 搜索类型: STALL, CAFETERIA, ALL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", length = 20, nullable = false)
    private SearchType searchType = SearchType.ALL;

    /**
     * 搜索结果数量
     */
    @Column(name = "result_count")
    private Integer resultCount = 0;

    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * 搜索时间
     */
    @Column(name = "searched_at", nullable = false, updatable = false)
    private LocalDateTime searchedAt;

    @PrePersist
    protected void onCreate() {
        searchedAt = LocalDateTime.now();
    }

    /**
     * 搜索类型枚举
     */
    public enum SearchType {
        STALL("摊位"),
        CAFETERIA("食堂"),
        ALL("全部");

        private final String displayName;

        SearchType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
