package com.example.eom.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "eom.notifications";

    public static final String ORDER_CONFIRMED_QUEUE = "eom.order.confirmed";
    public static final String ORDER_CONFIRMED_KEY   = "order.confirmed";

    public static final String ORDER_SHIPPED_QUEUE = "eom.order.shipped";
    public static final String ORDER_SHIPPED_KEY   = "order.shipped";

    @Bean
    DirectExchange notificationsExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue orderConfirmedQueue() {
        return new Queue(ORDER_CONFIRMED_QUEUE, true);
    }

    @Bean
    Queue orderShippedQueue() {
        return new Queue(ORDER_SHIPPED_QUEUE, true);
    }

    @Bean
    Binding orderConfirmedBinding(Queue orderConfirmedQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(orderConfirmedQueue).to(notificationsExchange).with(ORDER_CONFIRMED_KEY);
    }

    @Bean
    Binding orderShippedBinding(Queue orderShippedQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(orderShippedQueue).to(notificationsExchange).with(ORDER_SHIPPED_KEY);
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
