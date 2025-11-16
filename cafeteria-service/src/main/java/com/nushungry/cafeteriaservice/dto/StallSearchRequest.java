package com.nushungry.cafeteriaservice.dto;

import java.util.List;

/**
 * 摊位搜索请求DTO
 */
public class StallSearchRequest {

    /**
     * 关键词搜索（摊位名称、菜系类型）
     */
    private String keyword;

    /**
     * 菜系类型筛选（多选）
     */
    private List<String> cuisineTypes;

    /**
     * 最低评分
     */
    private Double minRating;

    /**
     * 是否只显示Halal食物
     */
    private Boolean halalOnly;

    /**
     * 食堂ID筛选
     */
    private Long cafeteriaId;

    /**
     * 用户位置经度（用于距离排序）
     */
    private Double userLongitude;

    /**
     * 用户位置纬度（用于距离排序）
     */
    private Double userLatitude;

    /**
     * 最大距离（km）
     */
    private Double maxDistance;

    /**
     * 排序方式：rating（评分），distance（距离），reviews（评价数）
     */
    private String sortBy;

    /**
     * 排序方向：asc, desc
     */
    private String sortDirection;

    /**
     * 页码（从0开始）
     */
    private Integer page = 0;

    /**
     * 每页大小
     */
    private Integer size = 20;

    public StallSearchRequest() {
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<String> getCuisineTypes() {
        return cuisineTypes;
    }

    public void setCuisineTypes(List<String> cuisineTypes) {
        this.cuisineTypes = cuisineTypes;
    }

    public Double getMinRating() {
        return minRating;
    }

    public void setMinRating(Double minRating) {
        this.minRating = minRating;
    }

    public Boolean getHalalOnly() {
        return halalOnly;
    }

    public void setHalalOnly(Boolean halalOnly) {
        this.halalOnly = halalOnly;
    }

    public Long getCafeteriaId() {
        return cafeteriaId;
    }

    public void setCafeteriaId(Long cafeteriaId) {
        this.cafeteriaId = cafeteriaId;
    }

    public Double getUserLongitude() {
        return userLongitude;
    }

    public void setUserLongitude(Double userLongitude) {
        this.userLongitude = userLongitude;
    }

    public Double getUserLatitude() {
        return userLatitude;
    }

    public void setUserLatitude(Double userLatitude) {
        this.userLatitude = userLatitude;
    }

    public Double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(Double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
