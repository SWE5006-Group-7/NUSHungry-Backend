package com.nushungry.mediaservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ImageStatsResponse {
    private Long totalImages;
    private Long totalSize;
    private Map<String, Long> typeDistribution;
    private Map<String, Long> uploadByDistribution;
    private Long todayCount;
    private Long thisWeekCount;
    private Long thisMonthCount;
}