package com.nushungry.userservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CafeteriaServiceClient 降级处理
 */
@Slf4j
@Component
public class CafeteriaServiceClientFallback implements CafeteriaServiceClient {

    @Override
    public CafeteriaStatsResponse getCafeteriaStats() {
        log.warn("CafeteriaService调用失败，使用降级数据");
        return CafeteriaStatsResponse.builder()
                .totalCafeterias(0)
                .yesterdayCafeterias(0)
                .totalStalls(0)
                .yesterdayStalls(0)
                .build();
    }
}
