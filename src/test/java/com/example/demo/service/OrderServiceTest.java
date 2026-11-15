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

    private OrderService orderService;

    private final String trainDate = LocalDate.now().plusDays(1).toString();

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, trainRepository, ticketRepository,
                stationRepository, userRepository, messageService);
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
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.buyTicket(9L, buyRequest());

        assertThat(order.getStatus()).isEqualTo("PAID");
        assertThat(order.getUserId()).isEqualTo(9L);
        assertThat(order.getSeatType()).isEqualTo("二等座");
        assertThat(order.getPrice()).isEqualTo(553.0);
        assertThat(order.getSeatNo()).isNotBlank();
        verify(messageService).sendSystemMessage(eq(9L), anyString(), nullable(Long.class));
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
