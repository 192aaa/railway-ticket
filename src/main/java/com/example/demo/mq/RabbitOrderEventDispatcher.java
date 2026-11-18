package com.example.demo.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

/** 把事件 ID 即时投递到 RabbitMQ（由 OrderEventDispatcherConfig 注册为 bean）。 */
public class RabbitOrderEventDispatcher implements OrderEventDispatcher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitOrderEventDispatcher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void dispatch(String eventId) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY, eventId);
    }
}
