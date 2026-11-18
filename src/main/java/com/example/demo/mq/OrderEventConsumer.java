package com.example.demo.mq;

import com.example.demo.service.OrderEventWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** RabbitMQ 消费者：收到事件 ID 后交给幂等 worker 处理。 */
@Component
@ConditionalOnProperty(name = "app.order.mq.enabled", havingValue = "true")
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderEventWorker worker;

    public OrderEventConsumer(OrderEventWorker worker) {
        this.worker = worker;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void onOrderCreated(String eventId) {
        log.info("收到订单事件 {}，开始履约", eventId);
        worker.process(eventId);
    }
}
