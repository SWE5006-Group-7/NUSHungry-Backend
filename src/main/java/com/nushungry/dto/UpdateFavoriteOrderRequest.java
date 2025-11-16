package com.nushungry.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateFavoriteOrderRequest {
    @Data
    public static class FavoriteOrder {
        private Long favoriteId;
        private Integer sortOrder;
    }

    private List<FavoriteOrder> orders;
}
