package com.nushungry.adminservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Mock
    private ConnectionFactory connectionFactory;

    @Test
    void shouldCreateDurableUserExchange() {
        TopicExchange exchange = config.userExchange();

        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.USER_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
    }

    @Test
    void shouldCreateDurableUserQueue() {
        Queue queue = config.userQueue();

        assertThat(queue.getName()).isEqualTo(RabbitMQConfig.USER_QUEUE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void shouldBindQueueToExchangeWithRoutingKey() {
        Binding binding = config.userBinding();

        assertThat(binding.getDestination()).isEqualTo(RabbitMQConfig.USER_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitMQConfig.USER_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.USER_ROUTING_KEY);
    }

    @Test
    void shouldCreateJacksonMessageConverter() {
        MessageConverter converter = config.jsonMessageConverter();

        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void shouldConfigureRabbitTemplateWithCustomConverter() {
        RabbitTemplate template = config.rabbitTemplate(connectionFactory);

        assertThat(template.getConnectionFactory()).isEqualTo(connectionFactory);
        assertThat(template.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
