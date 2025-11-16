package com.nushungry.preference.dto;

import lombok.Data;
import java.util.Map;

@Data
public class UpdateFavoriteOrderRequest {
    private Map<Long, Integer> orders; // favoriteId -> sortOrder
}