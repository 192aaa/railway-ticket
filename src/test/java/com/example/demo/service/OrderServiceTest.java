package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.BuyTicketRequest;
import com.example.demo.entity.Order;
import com.example.demo.entity.Train;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.StationRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.TrainRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private TrainRepository trainRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private StationRepository stationRepository;
    @Mock private UserRepository userRepository;
    @Mock private MessageService messageService;
    @Mock private TicketCacheVersion ticketCacheVersion;

    private OrderService orderService;

    private final String trainDate = LocalDate.now().plusDays(1).toString();

    @BeforeEach
    void setUp() {
        // 测试用事务管理器：直接执行回调、不做真实事务，使 retry 逻辑可测。
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
                stationRepository, userRepository, messageService, ticketCacheVersion, txManager);
    }

    @Test
    void buyTicketShouldFailWhenNoSeatLeft() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(train()));
        when(ticketRepository.deductSecondSeat(eq(1L), any(LocalDate.class))).thenReturn(0);

        assertThatThrownBy(() -> orderService.buyTicket(9L, buyRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("余票不足");

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(messageService);
    }

    @Test
    void buyTicketShouldCreatePaidOrderAndNotifyUser() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(train()));
        when(ticketRepository.deductSecondSeat(eq(1L), any(LocalDate.class))).thenReturn(1);
        when(stationRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(orderRepository.findByTrainIdAndTrainDate(eq(1L), anyString())).thenReturn(Collections.emptyList());
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.buyTicket(9L, buyRequest());

        assertThat(order.getStatus()).isEqualTo("PAID");
        assertThat(order.getUserId()).isEqualTo(9L);
        assertThat(order.getSeatType()).isEqualTo("二等座");
        assertThat(order.getPrice()).isEqualTo(553.0);
        assertThat(order.getSeatNo()).isNotBlank();
        verify(messageService).sendSystemMessage(eq(9L), anyString(), nullable(Long.class));
    }

    @Test
    void buyTicketShouldRetryWhenSeatConflictOccursOnce() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(train()));
        when(ticketRepository.deductSecondSeat(eq(1L), any(LocalDate.class))).thenReturn(1);
        when(stationRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(orderRepository.findByTrainIdAndTrainDate(eq(1L), anyString())).thenReturn(Collections.emptyList());
        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate seat"))
                .thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.buyTicket(9L, buyRequest());

        assertThat(order.getStatus()).isEqualTo("PAID");
        // 冲突一次后第二次成功：扣票与落单各执行两次
        verify(ticketRepository, times(2)).deductSecondSeat(eq(1L), any(LocalDate.class));
        verify(orderRepository, times(2)).saveAndFlush(any(Order.class));
    }

    @Test
    void buyTicketShouldFailAfterRepeatedSeatConflicts() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(train()));
        when(ticketRepository.deductSecondSeat(eq(1L), any(LocalDate.class))).thenReturn(1);
        when(stationRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(orderRepository.findByTrainIdAndTrainDate(eq(1L), anyString())).thenReturn(Collections.emptyList());
        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate seat"));

        assertThatThrownBy(() -> orderService.buyTicket(9L, buyRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("座位紧张");

        verify(orderRepository, times(8)).saveAndFlush(any(Order.class));
        verifyNoInteractions(messageService);
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
        order.setPrice(553.0);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(trainRepository.findById(1L)).thenReturn(Optional.of(train()));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order refunded = orderService.refundTicket(9L, 100L);

        assertThat(refunded.getStatus()).isEqualTo("REFUNDED");
        verify(ticketRepository).restoreSecondSeat(eq(1L), eq(LocalDate.parse(trainDate)));
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
