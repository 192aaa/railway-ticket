package com.example.demo.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ 拓扑：订单事件主题交换机 + 主队列 + 死信队列。仅在启用 MQ 时装配。 */
@Configuration
@ConditionalOnProperty(name = "app.order.mq.enabled", havingValue = "true")
public class RabbitMqConfig {

    public static final String EXCHANGE = "railway.order";
    public static final String ROUTING_KEY = "order.created";
    public static final String QUEUE = "railway.order.created";
    public static final String DLX = "railway.order.dlx";
    public static final String DLQ = "railway.order.dead";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        // 主队列消息过期/被拒后进入死信交换机，便于人工排查与补偿。
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey("order.dead")
                .build();
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(orderExchange()).with(ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderDeadExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue orderDeadQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding orderDeadBinding() {
        return BindingBuilder.bind(orderDeadQueue()).to(orderDeadExchange()).with("order.dead");
    }
}
