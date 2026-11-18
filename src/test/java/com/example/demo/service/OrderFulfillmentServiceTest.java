package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.OrderEventPayload;
import com.example.demo.entity.Order;
import com.example.demo.entity.Train;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentServiceTest {

    @Mock private com.example.demo.repository.OrderRepository orderRepository;
    @Mock private com.example.demo.repository.TrainRepository trainRepository;
    @Mock private com.example.demo.repository.TicketRepository ticketRepository;
    @Mock private MessageService messageService;
    @Mock private TicketCacheVersion ticketCacheVersion;

    private OrderFulfillmentService service;
    private final String trainDate = LocalDate.now().plusDays(1).toString();

    @BeforeEach
    void setUp() {
        PlatformTransactionManager txManager = new PlatformTransactionManager() {
            @Override
            public org.springframework.transaction.TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }
            @Override
            public void commit(org.springframework.transaction.TransactionStatus status) {}
            @Override
            public void rollback(org.springframework.transaction.TransactionStatus status) {}
        };
        service = new OrderFulfillmentService(orderRepository, trainRepository, ticketRepository,
                messageService, ticketCacheVersion, txManager);
    }

    @Test
    void fulfillDeductsSeatAssignsSeatAndPays() {
        Order order = pendingOrder();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(trainRepository.findById(1L)).thenReturn(Optional.of(train()));
        when(ticketRepository.deductSecondSeat(eq(1L), any(LocalDate.class))).thenReturn(1);
        when(orderRepository.findByTrainIdAndTrainDate(1L, trainDate)).thenReturn(Collections.emptyList());
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.fulfill(payload());

        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(result.getSeatNo()).isNotBlank();
        assertThat(result.getPayTime()).isNotNull();
        verify(messageService).sendSystemMessage(eq(9L), anyString(), eq(100L));
        verify(ticketCacheVersion).invalidate();
    }

    @Test
    void fulfillIsIdempotentForNonPendingOrder() {
        Order order = pendingOrder();
        order.setStatus("PAID");
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        Order result = service.fulfill(payload());

        assertThat(result.getStatus()).isEqualTo("PAID");
        verify(ticketRepository, never()).deductSecondSeat(any(), any());
        verify(messageService, never()).sendSystemMessage(any(), anyString(), any());
    }

    @Test
    void fulfillFailsWhenNoSeatLeft() {
        Order order = pendingOrder();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(trainRepository.findById(1L)).thenReturn(Optional.of(train()));
        when(ticketRepository.deductSecondSeat(eq(1L), any(LocalDate.class))).thenReturn(0);

        assertThatThrownBy(() -> service.fulfill(payload()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("余票不足");
        verify(orderRepository, never()).saveAndFlush(any());
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setId(100L);
        order.setUserId(9L);
        order.setTrainId(1L);
        order.setTrainNumber("G1");
        order.setFromStation("北京南");
        order.setToStation("上海虹桥");
        order.setTrainDate(trainDate);
        order.setSeatType("二等座");
        order.setStatus("PENDING");
        return order;
    }

    private Train train() {
        Train train = new Train();
        train.setId(1L);
        train.setTrainNumber("G1");
        train.setStartTime("08:00");
        return train;
    }

    private OrderEventPayload payload() {
        OrderEventPayload p = new OrderEventPayload();
        p.setEventId("evt-1");
        p.setOrderId(100L);
        p.setUserId(9L);
        p.setTrainId(1L);
        p.setTrainDate(trainDate);
        p.setSeatType("二等座");
        p.setPassengerName("张三");
        p.setPassengerIdCard("310101199001011234");
        return p;
    }
}
