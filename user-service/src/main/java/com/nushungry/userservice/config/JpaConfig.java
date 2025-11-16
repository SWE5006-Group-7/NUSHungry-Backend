package com.nushungry.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 审计配置
 *
 * 功能:
 * - 自动填充 @CreatedDate, @LastModifiedDate 等字段
 * - 从主应用类分离,便于测试时排除
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // JPA Auditing 配置
}
