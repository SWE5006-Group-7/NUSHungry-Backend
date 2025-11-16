package com.nushungry.userservice.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 客户端配置
 *
 * 功能:
 * - 启用 Feign 客户端扫描
 * - 从主应用类分离,便于测试时排除
 */
@Configuration
@EnableFeignClients(basePackages = "com.nushungry.userservice")
public class FeignConfig {
    // Feign 客户端配置
}
