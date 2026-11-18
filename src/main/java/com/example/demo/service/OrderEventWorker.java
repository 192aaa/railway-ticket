package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.OrderEventPayload;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderOutboxEvent;
import com.example.demo.entity.OutboxStatus;
import com.example.demo.repository.OrderOutboxRepository;
import com.example.demo.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事务发件箱消费者：把 PENDING 事件可靠地履约成 PAID 订单。
 * - 幂等：只有 PENDING 事件会被领取，重复投递直接跳过。
 * - 重试：失败按指数退避设置 nextRetryAt，超过 maxAttempts 进入 DEAD 死信。
 * - 自愈：进程崩溃遗留的 PROCESSING 事件超时后重置为 PENDING 重新消费。
 */
@Service
public class OrderEventWorker {

    private static final Logger log = LoggerFactory.getLogger(OrderEventWorker.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int BASE_BACKOFF_SECONDS = 2;
    private static final int MAX_BACKOFF_SECONDS = 300;
    private static final int STALLED_MINUTES = 5;

    private final OrderOutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final OrderFulfillmentService fulfillmentService;

    @Value("${app.order.worker.batch-size:20}")
    private int batchSize;

    public OrderEventWorker(OrderOutboxRepository outboxRepository,
                            OrderRepository orderRepository,
                            OrderFulfillmentService fulfillmentService) {
        this.outboxRepository = outboxRepository;
        this.orderRepository = orderRepository;
        this.fulfillmentService = fulfillmentService;
    }

    /** 定时拉取到期事件。无 MQ 时这是主消费通道；有 MQ 时作为兜底补偿。 */
    @Scheduled(fixedDelayString = "${app.order.worker.poll-ms:1000}")
    public void pollAndProcess() {
        List<OrderOutboxEvent> events = outboxRepository.findDueEvents(
                OutboxStatus.PENDING, LocalDateTime.now(), PageRequest.of(0, batchSize));
        for (OrderOutboxEvent event : events) {
            try {
                process(event.getEventId());
            } catch (Exception e) {
                log.warn("事件 {} 本轮处理失败，等待下次重试", event.getEventId(), e);
            }
        }
    }

    /** 处理单个事件，供定时轮询与 MQ 消费者共用（幂等）。 */
    @Transactional
    public boolean process(String eventId) {
        OrderOutboxEvent event = outboxRepository.findByEventId(eventId).orElse(null);
        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            return false;
        }

        // 领取事件：标记 PROCESSING，避免并发/重复投递导致重复扣票。
        event.setStatus(OutboxStatus.PROCESSING);
        outboxRepository.save(event);

        try {
            OrderEventPayload payload = OBJECT_MAPPER.readValue(event.getPayload(), OrderEventPayload.class);
            Order order = fulfillmentService.fulfill(payload);
            event.setStatus(OutboxStatus.DONE);
            event.setProcessedAt(LocalDateTime.now());
            event.setLastError(null);
            outboxRepository.save(event);
            log.info("事件 {} 履约完成，订单 {} 状态 {}", eventId, order.getOrderNo(), order.getStatus());
            return true;
        } catch (Exception e) {
            handleFailure(event, e);
            return false;
        }
    }

    private void handleFailure(OrderOutboxEvent event, Exception e) {
        String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (reason.length() > 500) {
            reason = reason.substring(0, 500);
        }
        event.setLastError(reason);
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        if (attempts >= event.getMaxAttempts()) {
            event.setStatus(OutboxStatus.DEAD);
            event.setNextRetryAt(LocalDateTime.now());
            log.error("事件 {} 重试 {} 次仍失败，进入死信：{}", event.getEventId(), attempts, reason);
        } else {
            long backoff = Math.min(BASE_BACKOFF_SECONDS << Math.min(attempts - 1, 8), MAX_BACKOFF_SECONDS);
            event.setStatus(OutboxStatus.PENDING);
            event.setNextRetryAt(LocalDateTime.now().plusSeconds(backoff));
            log.warn("事件 {} 第 {} 次失败，{} 秒后重试：{}", event.getEventId(), attempts, backoff, reason);
        }
        outboxRepository.save(event);
    }

    /** 把超时仍 PROCESSING 的事件重置为 PENDING，防止 worker 崩溃后事件永久卡住。 */
    @Scheduled(fixedDelayString = "${app.order.worker.recover-ms:30000}")
    @Transactional
    public void recoverStalled() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STALLED_MINUTES);
        List<OrderOutboxEvent> stalled = outboxRepository.findByStatusAndCreatedAtBefore(
                OutboxStatus.PROCESSING, threshold);
        for (OrderOutboxEvent event : stalled) {
            event.setStatus(OutboxStatus.PENDING);
            event.setNextRetryAt(LocalDateTime.now());
            outboxRepository.save(event);
            log.warn("事件 {} 疑似 worker 崩溃遗留，重置为 PENDING 重新消费", event.getEventId());
        }
    }

    /** 死信补偿：人工/管理端把 DEAD 事件重新入队。 */
    @Transactional
    public boolean requeueDead(String eventId) {
        OrderOutboxEvent event = outboxRepository.findByEventId(eventId).orElse(null);
        if (event == null || event.getStatus() != OutboxStatus.DEAD) {
            return false;
        }
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setNextRetryAt(LocalDateTime.now());
        event.setLastError(null);
        outboxRepository.save(event);
        log.info("死信事件 {} 已重新入队", eventId);
        return true;
    }
}
