package com.nushungry.adminservice.listener;

import com.nushungry.adminservice.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class UserEventListener {

    @RabbitListener(queues = RabbitMQConfig.USER_QUEUE)
    public void handleUserEvent(Map<String, Object> message) {
        String eventType = (String) message.get("eventType");
        String username = (String) message.get("username");
        String email = (String) message.get("email");
        Long userId = (Long) message.get("userId");
        
        log.info("Received user event: {} for user: {} (ID: {}) with email: {}", 
                eventType, username, userId, email);
        
        // 在这里可以实现缓存更新、日志记录等操作
        switch (eventType) {
            case "CREATED":
                handleUserCreated(message);
                break;
            case "UPDATED":
                handleUserUpdated(message);
                break;
            case "DELETED":
                handleUserDeleted(message);
                break;
            default:
                log.warn("Unknown user event type: {}", eventType);
        }
    }

    private void handleUserCreated(Map<String, Object> message) {
        log.info("Processing user created event: {}", message);
        // 可以在这里实现缓存更新等操作
    }

    private void handleUserUpdated(Map<String, Object> message) {
        log.info("Processing user updated event: {}", message);
        // 可以在这里实现缓存更新等操作
    }

    private void handleUserDeleted(Map<String, Object> message) {
        log.info("Processing user deleted event: {}", message);
        // 可以在这里实现缓存清理等操作
    }
}