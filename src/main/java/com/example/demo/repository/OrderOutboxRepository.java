package com.example.demo.repository;

import com.example.demo.entity.OrderOutboxEvent;
import com.example.demo.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderOutboxRepository extends JpaRepository<OrderOutboxEvent, Long> {

    Optional<OrderOutboxEvent> findByEventId(String eventId);

    /** 拉取到期事件，按创建时间顺序；消费幂等由 worker 的状态流转保证。 */
    @Query("SELECT e FROM OrderOutboxEvent e WHERE e.status = :status AND e.nextRetryAt <= :now ORDER BY e.createdAt ASC")
    List<OrderOutboxEvent> findDueEvents(@Param("status") OutboxStatus status,
                                         @Param("now") LocalDateTime now,
                                         Pageable pageable);

    /** 标记某订单是否已有未完成的 OUTBOX 事件（下单幂等）。 */
    boolean existsByOrderIdAndEventTypeAndStatusNot(Long orderId, String eventType, OutboxStatus status);

    /** 查找领取后超时未完成的 PROCESSING 事件（worker 崩溃遗留）。 */
    List<OrderOutboxEvent> findByStatusAndCreatedAtBefore(OutboxStatus status, LocalDateTime threshold);

    List<OrderOutboxEvent> findByStatus(OutboxStatus status);

    long countByStatus(OutboxStatus status);
}
