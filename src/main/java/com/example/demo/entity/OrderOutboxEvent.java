package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事务发件箱：下单与事件记录在同一个本地事务里落库，
 * 异步 worker 再可靠投递，避免"订单成功但消息丢失"。
 */
@Data
@Entity
@Table(name = "order_outbox", indexes = {
        @Index(name = "idx_outbox_status", columnList = "status,next_retry_at"),
        @Index(name = "idx_outbox_event_id", columnList = "event_id", unique = true)
})
public class OrderOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 全局唯一事件 ID，用于消费端幂等去重。 */
    @Column(name = "event_id", nullable = false, length = 40)
    private String eventId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    /** 事件负载（JSON），保存下单快照，消费端不依赖订单表当前状态。 */
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 8;

    /** 下次可处理时间，用于失败退避；PENDING 且到期才被拉取。 */
    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt = LocalDateTime.now();

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** 乐观锁：多 worker / MQ 并发消费时防止重复履约。 */
    @Version
    @Column(name = "version", nullable = false)
    private long version = 0;
}
