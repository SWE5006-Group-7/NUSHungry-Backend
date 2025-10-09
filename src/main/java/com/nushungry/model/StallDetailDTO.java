package com.nushungry.model;

import java.util.List;

public class StallDetailDTO {
    private Long id;
    private String name;
    private String cuisineType;
    private String imageUrl;
    private String halalInfo;
    private String contact;
    private Long cafeteriaId;
    private String cafeteriaName;
    private List<Review> reviews;
    private List<Image> images;

    public StallDetailDTO(Long id, String name, String cuisineType, String imageUrl,
                         String halalInfo, String contact) {
        this.id = id;
        this.name = name;
        this.cuisineType = cuisineType;
        this.imageUrl = imageUrl;
        this.halalInfo = halalInfo;
        this.contact = contact;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCuisineType() { return cuisineType; }
    public void setCuisineType(String cuisineType) { this.cuisineType = cuisineType; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getHalalInfo() { return halalInfo; }
    public void setHalalInfo(String halalInfo) { this.halalInfo = halalInfo; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public Long getCafeteriaId() { return cafeteriaId; }
    public void setCafeteriaId(Long cafeteriaId) { this.cafeteriaId = cafeteriaId; }
    public String getCafeteriaName() { return cafeteriaName; }
    public void setCafeteriaName(String cafeteriaName) { this.cafeteriaName = cafeteriaName; }
    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
    public List<Image> getImages() { return images; }
    public void setImages(List<Image> images) { this.images = images; }
}