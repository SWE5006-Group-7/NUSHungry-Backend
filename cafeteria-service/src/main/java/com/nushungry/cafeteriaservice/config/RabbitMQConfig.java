package com.nushungry.cafeteriaservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 修改为与 review-service 一致的队列名称
    public static final String REVIEW_EVENT_QUEUE = "review.rating.queue";
    public static final String PRICE_EVENT_QUEUE = "review.price.queue";

    /**
     * 声明评分变更队列
     * 参数必须与 review-service 的队列配置完全一致
     */
    @Bean
    public Queue reviewEventQueue() {
        return QueueBuilder.durable(REVIEW_EVENT_QUEUE)
                .withArgument("x-message-ttl", 86400000)  // 消息TTL: 24小时
                .withArgument("x-max-length", 10000)      // 队列最大长度
                .build();
    }

    /**
     * 声明价格变更队列
     * 参数必须与 review-service 的队列配置完全一致
     */
    @Bean
    public Queue priceEventQueue() {
        return QueueBuilder.durable(PRICE_EVENT_QUEUE)
                .withArgument("x-message-ttl", 86400000)  // 消息TTL: 24小时
                .withArgument("x-max-length", 10000)      // 队列最大长度
                .build();
    }
}


