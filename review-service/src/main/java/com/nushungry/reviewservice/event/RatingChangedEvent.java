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
public class RatingChangedEvent {

    private Long stallId;
    private Double newAverageRating;
    private Long reviewCount;
    private LocalDateTime timestamp;
}
