package com.nushungry.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Stall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "cuisine_type")
    private String cuisineType;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "halal_info")
    private String halalInfo;

    private String contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafeteria_id", nullable = true)
    @JsonBackReference
    private Cafeteria cafeteria;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Review> reviews;
}