package com.nushungry.model;

import java.util.List;
import java.util.Set;

public class CafeteriaDetailDTO {
    private Long id;
    private List<Stall> stalls;
    private String name;
    private String description;
    private String location;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private String termTimeOpeningHours;
    private String vacationOpeningHours;
    private String nearestBusStop;
    private String nearestCarpark;
    private String halalInfo;
    private Integer seatingCapacity;
    private Set<String> cuisineTags; // 从 Stall 聚合的标签
    private List<Image> images; // 图片列表

    public CafeteriaDetailDTO(Long id, String name, String description, String location, Double latitude, Double longitude, String imageUrl, String termTimeOpeningHours, String vacationOpeningHours, String nearestBusStop, String nearestCarpark, String halalInfo, Integer seatingCapacity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.termTimeOpeningHours = termTimeOpeningHours;
        this.vacationOpeningHours = vacationOpeningHours;
        this.nearestBusStop = nearestBusStop;
        this.nearestCarpark = nearestCarpark;
        this.halalInfo = halalInfo;
        this.seatingCapacity = seatingCapacity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public List<Stall> getStalls() { return stalls; }
    public void setStalls(List<Stall> stalls) { this.stalls = stalls; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getTermTimeOpeningHours() { return termTimeOpeningHours; }
    public void setTermTimeOpeningHours(String termTimeOpeningHours) { this.termTimeOpeningHours = termTimeOpeningHours; }
    public String getVacationOpeningHours() { return vacationOpeningHours; }
    public void setVacationOpeningHours(String vacationOpeningHours) { this.vacationOpeningHours = vacationOpeningHours; }
    public String getNearestBusStop() { return nearestBusStop; }
    public void setNearestBusStop(String nearestBusStop) { this.nearestBusStop = nearestBusStop; }
    public String getNearestCarpark() { return nearestCarpark; }
    public void setNearestCarpark(String nearestCarpark) { this.nearestCarpark = nearestCarpark; }
    public String getHalalInfo() { return halalInfo; }
    public void setHalalInfo(String halalInfo) { this.halalInfo = halalInfo; }
    public Integer getSeatingCapacity() { return seatingCapacity; }
    public void setSeatingCapacity(Integer seatingCapacity) { this.seatingCapacity = seatingCapacity; }
    public Set<String> getCuisineTags() { return cuisineTags; }
    public void setCuisineTags(Set<String> cuisineTags) { this.cuisineTags = cuisineTags; }
    public List<Image> getImages() { return images; }
    public void setImages(List<Image> images) { this.images = images; }
}
