package com.nushungry.reviewservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceChangedEvent {

    private Long stallId;
    private Double newAveragePrice;
    private Long priceCount;
    private LocalDateTime timestamp;
}
