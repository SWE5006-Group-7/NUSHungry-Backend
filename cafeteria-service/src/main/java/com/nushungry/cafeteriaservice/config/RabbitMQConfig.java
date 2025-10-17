package com.nushungry.cafeteriaservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String REVIEW_EVENT_QUEUE = "review.events";

    @Bean
    public Queue reviewEventQueue() {
        return new Queue(REVIEW_EVENT_QUEUE, true);
    }
}


