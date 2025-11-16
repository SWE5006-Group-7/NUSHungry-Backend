package com.nushungry.userservice.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Feign客户端 - 调用Cafeteria Service
 */
@FeignClient(
    name = "cafeteria-service",
    url = "${services.cafeteria-service.url}",
    fallback = CafeteriaServiceClientFallback.class
)
public interface CafeteriaServiceClient {

    /**
     * 获取食堂统计信息
     */
    @GetMapping("/admin/dashboard/stats")
    CafeteriaStatsResponse getCafeteriaStats();

    /**
     * 食堂统计响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "食堂统计响应")
    class CafeteriaStatsResponse {
        @Schema(description = "总食堂数")
        private Integer totalCafeterias;

        @Schema(description = "昨天的食堂数")
        private Integer yesterdayCafeterias;

        @Schema(description = "总档口数")
        private Integer totalStalls;

        @Schema(description = "昨天的档口数")
        private Integer yesterdayStalls;
    }
}
