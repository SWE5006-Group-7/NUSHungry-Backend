package com.nushungry.reviewservice.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 测试配置类 - 禁用MongoDB和RabbitMQ相关自动配置
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class
})
@ComponentScan(
    basePackages = "com.nushungry.reviewservice",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {MongoConfig.class, RabbitMQConfig.class}
        )
    }
)
public class TestConfig {
    // 测试配置类,用于排除MongoDB和RabbitMQ配置
}
