package com.nushungry.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 搜索历史记录实体
 */
@Data
@Entity
@Table(name = "search_history", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_search_time", columnList = "search_time")
})
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID（可以为null，表示匿名用户）
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 搜索关键词
     */
    @Column(name = "keyword", nullable = false, length = 255)
    private String keyword;

    /**
     * 搜索时间
     */
    @Column(name = "search_time", nullable = false)
    private LocalDateTime searchTime;

    /**
     * 搜索类型（stall, cafeteria, all）
     */
    @Column(name = "search_type", length = 50)
    private String searchType;

    /**
     * 搜索结果数量
     */
    @Column(name = "result_count")
    private Integer resultCount;

    /**
     * 用户IP地址
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        if (searchTime == null) {
            searchTime = LocalDateTime.now();
        }
    }
}
