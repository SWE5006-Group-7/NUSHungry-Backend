package com.nushungry.reviewservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * RabbitMQ 配置类
 * 配置交换机、队列、绑定关系和消息模板
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.review}")
    private String reviewExchange;

    @Value("${rabbitmq.routing-key.rating-changed}")
    private String ratingChangedRoutingKey;

    @Value("${rabbitmq.routing-key.price-changed}")
    private String priceChangedRoutingKey;

    @Value("${rabbitmq.queue.rating}")
    private String ratingQueue;

    @Value("${rabbitmq.queue.price}")
    private String priceQueue;

    /**
     * 创建 Topic 交换机
     * 持久化交换机，服务器重启后不会丢失
     */
    @Bean
    public TopicExchange reviewExchange() {
        return new TopicExchange(reviewExchange, true, false);
    }

    /**
     * 创建评分变更队列
     * 参数说明:
     * - durable: true (持久化)
     * - exclusive: false (非排他)
     * - autoDelete: false (非自动删除)
     */
    @Bean
    public Queue ratingQueue() {
        return QueueBuilder.durable(ratingQueue)
                .withArgument("x-message-ttl", 86400000)  // 消息TTL: 24小时
                .withArgument("x-max-length", 10000)      // 队列最大长度
                .build();
    }

    /**
     * 创建价格变更队列
     */
    @Bean
    public Queue priceQueue() {
        return QueueBuilder.durable(priceQueue)
                .withArgument("x-message-ttl", 86400000)  // 消息TTL: 24小时
                .withArgument("x-max-length", 10000)      // 队列最大长度
                .build();
    }

    /**
     * 绑定评分队列到交换机
     */
    @Bean
    public Binding ratingBinding() {
        return BindingBuilder
                .bind(ratingQueue())
                .to(reviewExchange())
                .with(ratingChangedRoutingKey);
    }

    /**
     * 绑定价格队列到交换机
     */
    @Bean
    public Binding priceBinding() {
        return BindingBuilder
                .bind(priceQueue())
                .to(reviewExchange())
                .with(priceChangedRoutingKey);
    }

    /**
     * 消息转换器 - 使用 Jackson2 JSON 格式
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate
     * 添加发布确认和消息返回回调
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        
        // 消息发送确认回调（生产环境重要）
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("消息发送成功: {}", correlationData);
            } else {
                log.error("消息发送失败: {}, 原因: {}", correlationData, cause);
            }
        });
        
        // 消息返回回调（消息无法路由时触发）
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息无法路由到队列: exchange={}, routingKey={}, replyText={}", 
                    returned.getExchange(), 
                    returned.getRoutingKey(), 
                    returned.getReplyText());
        });
        
        return rabbitTemplate;
    }
}
