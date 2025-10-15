package com.nushungry.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteFavoritesRequest {
    private List<Long> favoriteIds;
}
