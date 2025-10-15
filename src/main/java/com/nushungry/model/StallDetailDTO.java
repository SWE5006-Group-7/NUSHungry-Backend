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
    private Double latitude;  // Stall自己的坐标
    private Double longitude;
    private CafeteriaBasicDTO cafeteria;  // Cafeteria完整信息(包含坐标)
    private Double averageRating;  // 平均评分
    private Integer reviewCount;  // 评价数量
    private Double averagePrice;  // 人均价格
    private List<Review> reviews;
    private List<Image> images;

    // 内部类：Cafeteria基本信息DTO
    public static class CafeteriaBasicDTO {
        private Long id;
        private String name;
        private String location;
        private Double latitude;
        private Double longitude;

        public CafeteriaBasicDTO(Long id, String name, String location, Double latitude, Double longitude) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }

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
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public CafeteriaBasicDTO getCafeteria() { return cafeteria; }
    public void setCafeteria(CafeteriaBasicDTO cafeteria) { this.cafeteria = cafeteria; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public Double getAveragePrice() { return averagePrice; }
    public void setAveragePrice(Double averagePrice) { this.averagePrice = averagePrice; }
    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
    public List<Image> getImages() { return images; }
    public void setImages(List<Image> images) { this.images = images; }
}