package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderOutboxEvent;
import com.example.demo.entity.OutboxStatus;
import com.example.demo.repository.OrderOutboxRepository;
import com.example.demo.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventWorkerTest {

    @Mock private OrderOutboxRepository outboxRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderFulfillmentService fulfillmentService;

    private OrderEventWorker worker;

    @BeforeEach
    void setUp() {
        worker = new OrderEventWorker(outboxRepository, orderRepository, fulfillmentService);
        ReflectionTestUtils.setField(worker, "batchSize", 20);
    }

    @Test
    void processMarksEventDoneOnSuccess() {
        OrderOutboxEvent event = event(OutboxStatus.PENDING);
        when(outboxRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));
        when(fulfillmentService.fulfill(any())).thenReturn(paidOrder());

        assertThat(worker.process("evt-1")).isTrue();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DONE);
        assertThat(event.getProcessedAt()).isNotNull();
        verify(fulfillmentService).fulfill(any());
    }

    @Test
    void processSkipsNonPendingEvent() {
        OrderOutboxEvent event = event(OutboxStatus.DONE);
        when(outboxRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));

        assertThat(worker.process("evt-1")).isFalse();
        verify(fulfillmentService, never()).fulfill(any());
    }

    @Test
    void processRetriesWithBackoffOnFailure() {
        OrderOutboxEvent event = event(OutboxStatus.PENDING);
        when(outboxRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));
        when(fulfillmentService.fulfill(any())).thenThrow(new IllegalStateException("余票不足"));

        assertThat(worker.process("evt-1")).isFalse();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isAfter(event.getCreatedAt());
        assertThat(event.getLastError()).contains("余票不足");
    }

    @Test
    void processMovesToDeadAfterMaxAttempts() {
        OrderOutboxEvent event = event(OutboxStatus.PENDING);
        event.setAttempts(7);
        event.setMaxAttempts(8);
        when(outboxRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));
        when(fulfillmentService.fulfill(any())).thenThrow(new IllegalStateException("持续失败"));

        assertThat(worker.process("evt-1")).isFalse();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(event.getAttempts()).isEqualTo(8);
    }

    @Test
    void requeueDeadLetterResetsEvent() {
        OrderOutboxEvent event = event(OutboxStatus.DEAD);
        event.setAttempts(8);
        when(outboxRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));

        assertThat(worker.requeueDead("evt-1")).isTrue();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void requeueRejectsNonDeadEvent() {
        OrderOutboxEvent event = event(OutboxStatus.DONE);
        when(outboxRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));

        assertThat(worker.requeueDead("evt-1")).isFalse();
        verify(outboxRepository, never()).save(any());
    }

    private OrderOutboxEvent event(OutboxStatus status) {
        OrderOutboxEvent event = new OrderOutboxEvent();
        event.setEventId("evt-1");
        event.setOrderId(100L);
        event.setEventType(OrderEventTypes.ORDER_CREATED);
        event.setStatus(status);
        event.setPayload("{\"eventId\":\"evt-1\",\"orderId\":100,\"userId\":9,\"trainId\":1,"
                + "\"trainDate\":\"" + LocalDate.now().plusDays(1) + "\",\"seatType\":\"二等座\"}");
        return event;
    }

    private Order paidOrder() {
        Order order = new Order();
        order.setId(100L);
        order.setOrderNo("ORD123");
        order.setStatus("PAID");
        return order;
    }
}
