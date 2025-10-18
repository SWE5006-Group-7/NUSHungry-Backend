package com.nushungry.preference.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "favorite")
public class Favorite implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long stallId;

    @Column(nullable = false)
    private Long createdAt;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getStallId() { return stallId; }
    public void setStallId(Long stallId) { this.stallId = stallId; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}

