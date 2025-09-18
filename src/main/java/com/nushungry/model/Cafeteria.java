package com.nushungry.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
public class Cafeteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String location;        // e.g., "UTown", "Faculty of Engineering"
    private double latitude;
    private double longitude;

    private String nearestCarpark;
    private String nearestBusStop;

    private String halalInfo;        // HALAL or VEGETARIAN info
    private Integer seatingCapacity;
    private String imageUrl;
    private String termTimeOpeningHours;
    private String vacationOpeningHours;

    @OneToMany(mappedBy = "cafeteria", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Stall> stalls;
}