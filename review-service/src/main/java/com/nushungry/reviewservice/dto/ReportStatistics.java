package com.nushungry.reviewservice.dto;

import com.nushungry.reviewservice.enums.ReportReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 举报统计数据类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatistics {

    /**
     * 总举报数量
     */
    private long totalCount;

    /**
     * 待处理举报数量
     */
    private long pendingCount;

    /**
     * 处理中举报数量
     */
    private long reviewingCount;

    /**
     * 已处理举报数量
     */
    private long processedCount;

    /**
     * 已拒绝举报数量
     */
    private long rejectedCount;

    /**
     * 今日举报数量
     */
    private long todayCount;

    /**
     * 本周举报数量
     */
    private long thisWeekCount;

    /**
     * 本月举报数量
     */
    private long thisMonthCount;

    /**
     * 按原因分布的统计数据
     */
    @Builder.Default
    private Map<String, Long> reasonDistribution = new HashMap<>();

    /**
     * 按状态分布的统计数据
     */
    @Builder.Default
    private Map<String, Long> statusDistribution = new HashMap<>();

    /**
     * 平均处理时间（小时）
     */
    private Double averageHandlingTimeHours;

    /**
     * 处理率（已处理/总数）
     */
    private Double processingRate;

    /**
     * 转化为百分比的处理率
     */
    public Double getProcessingRatePercentage() {
        if (totalCount == 0) return 0.0;
        return (processingRate != null ? processingRate : 0.0) * 100;
    }
}