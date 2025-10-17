package com.nushungry.cafeteriaservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.List;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    @JoinColumn(name = "cafeteria_id")
    @JsonBackReference
    private Cafeteria cafeteria;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("stall-images")
    private List<Image> images;

    @Column(name = "avg_rating")
    private Double avgRating;

    @Column(name = "avg_price")
    private Double avgPrice;

    public Stall() {
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

    public String getCuisineType() {
        return cuisineType;
    }

    public void setCuisineType(String cuisineType) {
        this.cuisineType = cuisineType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getHalalInfo() {
        return halalInfo;
    }

    public void setHalalInfo(String halalInfo) {
        this.halalInfo = halalInfo;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public Cafeteria getCafeteria() {
        return cafeteria;
    }

    public void setCafeteria(Cafeteria cafeteria) {
        this.cafeteria = cafeteria;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public Double getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(Double avgRating) {
        this.avgRating = avgRating;
    }

    public Double getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(Double avgPrice) {
        this.avgPrice = avgPrice;
    }
}


