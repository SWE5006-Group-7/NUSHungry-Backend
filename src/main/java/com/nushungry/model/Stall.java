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
    private String cuisineType; // e.g., "Chinese", "Western", "Indian"

    private String imageUrl;
    private String halalInfo;
    private String termTimeOpeningHours;
    private String vacationOpeningHours;
    private Integer seatingCapacity;
    private String contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafeteria_id", nullable = true)
    @JsonBackReference
    private Cafeteria cafeteria;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Review> reviews;
}