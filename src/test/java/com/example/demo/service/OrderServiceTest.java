package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.BuyTicketRequest;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderOutboxEvent;
import com.example.demo.entity.Station;
import com.example.demo.entity.Train;
import com.example.demo.mq.OrderEventDispatcher;
import com.example.demo.repository.OrderOutboxRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.StationRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.TrainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private TrainRepository trainRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private StationRepository stationRepository;
    @Mock private OrderOutboxRepository orderOutboxRepository;
    @Mock private MessageService messageService;
    @Mock private TicketCacheVersion ticketCacheVersion;
    @Mock private OrderEventDispatcher eventDispatcher;

    private OrderService orderService;
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
        orderService = new OrderService(orderRepository, trainRepository, ticketRepository,
                stationRepository, orderOutboxRepository, messageService, ticketCacheVersion,
                eventDispatcher, txManager);
    }

    @Test
    void buyTicketCreatesPendingOrderAndOutboxEvent() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(train()));
        when(stationRepository.findById(anyLong())).thenReturn(Optional.of(station("北京南")));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });

        Order order = orderService.buyTicket(9L, buyRequest());

        // 下单只创建 PENDING 订单，不直接扣票/选座/发通知。
        assertThat(order.getStatus()).isEqualTo("PENDING");
        assertThat(order.getUserId()).isEqualTo(9L);
        assertThat(order.getSeatNo()).isNull();
        assertThat(order.getPrice()).isEqualTo(553.0);
        verify(ticketRepository, never()).deductSecondSeat(any(), any());
        verify(messageService, never()).sendSystemMessage(any(), anyString(), any());

        // 同一事务写入一条 ORDER_CREATED 事件。
        ArgumentCaptor<OrderOutboxEvent> captor = ArgumentCaptor.forClass(OrderOutboxEvent.class);
        verify(orderOutboxRepository).save(captor.capture());
        OrderOutboxEvent event = captor.getValue();
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getOrderId()).isEqualTo(100L);
        assertThat(event.getEventType()).isEqualTo(OrderEventTypes.ORDER_CREATED);
        assertThat(event.getPayload()).contains("\"orderId\":100");
    }

    @Test
    void buyTicketRejectsUnknownTrain() {
        when(trainRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.buyTicket(9L, buyRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("车次不存在");
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void buyTicketRejectsUnknownSeatType() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(train()));
        BuyTicketRequest req = buyRequest();
        req.setSeatType("站票");

        assertThatThrownBy(() -> orderService.buyTicket(9L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的座位类型");
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void refundTicketShouldRejectOtherUsersOrder() {
        Order order = new Order();
        order.setId(100L);
        order.setUserId(1L);
        order.setStatus("PAID");
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.refundTicket(2L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权操作此订单");
        verify(ticketRepository, never()).restoreSecondSeat(any(), any());
    }

    @Test
    void refundTicketShouldRestoreSeatAndUpdateStatus() {
        Order order = new Order();
        order.setId(100L);
        order.setUserId(9L);
        order.setTrainId(1L);
        order.setTrainDate(trainDate);
        order.setSeatType("二等座");
        order.setStatus("PAID");
        order.setSeatNo("5A");
        order.setPrice(553.0);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(trainRepository.findById(1L)).thenReturn(Optional.of(train()));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order refunded = orderService.refundTicket(9L, 100L);

        assertThat(refunded.getStatus()).isEqualTo("REFUNDED");
        assertThat(refunded.getSeatNo()).contains("#F");
        verify(ticketRepository).restoreSecondSeat(eq(1L), eq(LocalDate.parse(trainDate)));
    }

    @Test
    void refundTicketRejectsPendingOrder() {
        Order order = new Order();
        order.setId(100L);
        order.setUserId(9L);
        order.setStatus("PENDING");
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.refundTicket(9L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前状态不可退票");
    }

    private Train train() {
        Train train = new Train();
        train.setId(1L);
        train.setTrainNumber("G1");
        train.setFromStationId(1L);
        train.setToStationId(2L);
        train.setStartTime("23:59");
        train.setSecondSeatPrice(553.0);
        return train;
    }

    private Station station(String name) {
        Station station = new Station();
        station.setId(1L);
        station.setName(name);
        return station;
    }

    private BuyTicketRequest buyRequest() {
        BuyTicketRequest req = new BuyTicketRequest();
        req.setTrainId(1L);
        req.setTrainDate(trainDate);
        req.setSeatType("二等座");
        req.setPassengerName("张三");
        req.setPassengerIdCard("310101199001011234");
        return req;
    }
}
