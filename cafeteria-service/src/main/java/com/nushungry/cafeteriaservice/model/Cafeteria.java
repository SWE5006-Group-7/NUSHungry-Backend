package com.nushungry.cafeteriaservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.List;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Cafeteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String location;
    private double latitude;
    private double longitude;

    private String nearestCarpark;
    private String nearestBusStop;

    private String halalInfo;
    private Integer seatingCapacity;
    private String imageUrl;
    private String termTimeOpeningHours;
    private String vacationOpeningHours;

    @OneToMany(mappedBy = "cafeteria", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("cafeteria-images")
    private List<Image> images;

    @OneToMany(mappedBy = "cafeteria", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Stall> stalls;

    public Cafeteria() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getNearestCarpark() {
        return nearestCarpark;
    }

    public void setNearestCarpark(String nearestCarpark) {
        this.nearestCarpark = nearestCarpark;
    }

    public String getNearestBusStop() {
        return nearestBusStop;
    }

    public void setNearestBusStop(String nearestBusStop) {
        this.nearestBusStop = nearestBusStop;
    }

    public String getHalalInfo() {
        return halalInfo;
    }

    public void setHalalInfo(String halalInfo) {
        this.halalInfo = halalInfo;
    }

    public Integer getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(Integer seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTermTimeOpeningHours() {
        return termTimeOpeningHours;
    }

    public void setTermTimeOpeningHours(String termTimeOpeningHours) {
        this.termTimeOpeningHours = termTimeOpeningHours;
    }

    public String getVacationOpeningHours() {
        return vacationOpeningHours;
    }

    public void setVacationOpeningHours(String vacationOpeningHours) {
        this.vacationOpeningHours = vacationOpeningHours;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public List<Stall> getStalls() {
        return stalls;
    }

    public void setStalls(List<Stall> stalls) {
        this.stalls = stalls;
    }
}


