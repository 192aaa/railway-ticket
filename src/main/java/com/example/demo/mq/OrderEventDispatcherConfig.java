package com.example.demo.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** 按配置选择订单事件投递通道：默认本地（worker 轮询兜底），启用 MQ 时走 RabbitMQ。 */
@Configuration
public class OrderEventDispatcherConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.order.mq.enabled", havingValue = "false", matchIfMissing = true)
    public OrderEventDispatcher localOrderEventDispatcher() {
        return eventId -> { /* no-op：发件箱由 worker 定时轮询消费 */ };
    }

    @Bean
    @ConditionalOnProperty(name = "app.order.mq.enabled", havingValue = "true")
    public OrderEventDispatcher rabbitOrderEventDispatcher(RabbitTemplate rabbitTemplate) {
        return new RabbitOrderEventDispatcher(rabbitTemplate);
    }
}
