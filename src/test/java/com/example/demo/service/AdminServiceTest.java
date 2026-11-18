package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.entity.Station;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.StationRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.TrainRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private StationRepository stationRepository;
    @Mock private TrainRepository trainRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private TicketCacheVersion ticketCacheVersion;
    @InjectMocks private AdminService adminService;

    @Test
    void updateRoleRejectsUnknownRole() {
        assertThatThrownBy(() -> adminService.updateUserRole(1L, "SUPERUSER"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("角色只能是 USER 或 ADMIN");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateRolePromotesUser() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = adminService.updateUserRole(1L, UserRole.ADMIN);

        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void deleteStationRejectsReferencedStation() {
        when(trainRepository.existsByFromStationIdOrToStationId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> adminService.deleteStation(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该站点仍被车次引用，无法删除");
        verify(stationRepository, never()).deleteById(any());
    }

    @Test
    void adjustSeatRejectsNegativeResult() {
        Ticket ticket = new Ticket();
        ticket.setTrainId(1L);
        ticket.setTrainDate(LocalDate.of(2026, 11, 18));
        ticket.setSecondSeatCount(2);
        when(ticketRepository.findByTrainIdAndTrainDate(1L, LocalDate.of(2026, 11, 18)))
                .thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> adminService.adjustSeat(1L, LocalDate.of(2026, 11, 18), "二等座", -3))
                .isInstanceOf(BusinessException.class)
                .hasMessage("余票不足，不能扣减为负数");
        verify(ticketCacheVersion, never()).invalidate();
    }

    @Test
    void adjustSeatInvalidatesCacheOnSuccess() {
        Ticket ticket = new Ticket();
        ticket.setTrainId(1L);
        ticket.setTrainDate(LocalDate.of(2026, 11, 18));
        ticket.setSecondSeatCount(5);
        when(ticketRepository.findByTrainIdAndTrainDate(1L, LocalDate.of(2026, 11, 18)))
                .thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket result = adminService.adjustSeat(1L, LocalDate.of(2026, 11, 18), "二等座", 2);

        assertThat(result.getSecondSeatCount()).isEqualTo(7);
        verify(ticketCacheVersion).invalidate();
    }
}
