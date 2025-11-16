package com.nushungry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponse {
    private Long favoriteId;
    private Long stallId;
    private String stallName;
    private String stallImage;
    private List<String> stallImages;
    private String cuisineType;
    private Boolean halal;
    private Double averageRating;
    private Integer reviewCount;
    private Long cafeteriaId;
    private String cafeteriaName;
    private String cafeteriaLocation;
    private LocalDateTime createdAt;
    private Integer sortOrder;
}
