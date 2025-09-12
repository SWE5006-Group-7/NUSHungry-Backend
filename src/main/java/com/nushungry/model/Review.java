package com.nushungry.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String author; // In a real app, this would be a User entity
    private int rating; // e.g., 1 to 5
    private String comment;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stall_id")
    @JsonBackReference
    private Stall stall;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}