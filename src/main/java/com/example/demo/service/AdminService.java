package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.entity.Order;
import com.example.demo.entity.SeatType;
import com.example.demo.entity.Station;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.Train;
import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.StationRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.TrainRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 管理端聚合服务：用户、订单、站点、车次与余票维护。 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final StationRepository stationRepository;
    private final TrainRepository trainRepository;
    private final TicketRepository ticketRepository;
    private final TicketCacheVersion ticketCacheVersion;

    public AdminService(UserRepository userRepository,
                        OrderRepository orderRepository,
                        StationRepository stationRepository,
                        TrainRepository trainRepository,
                        TicketRepository ticketRepository,
                        TicketCacheVersion ticketCacheVersion) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.stationRepository = stationRepository;
        this.trainRepository = trainRepository;
        this.ticketRepository = ticketRepository;
        this.ticketCacheVersion = ticketCacheVersion;
    }

    // ---------- 用户 ----------

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUserRole(Long userId, String role) {
        if (!UserRole.isValid(role)) {
            throw new BusinessException("角色只能是 USER 或 ADMIN");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setRole(role);
        return userRepository.save(user);
    }

    // ---------- 订单 ----------

    public List<Order> listOrders(String status) {
        return status == null || status.isBlank()
                ? orderRepository.findAll()
                : orderRepository.findByStatus(status);
    }

    // ---------- 站点 ----------

    public List<Station> listStations() {
        return stationRepository.findAll();
    }

    @Transactional
    public Station saveStation(Station station) {
        if (station.getName() == null || station.getName().isBlank()) {
            throw new BusinessException("站点名称不能为空");
        }
        Station existing = stationRepository.findByName(station.getName().trim());
        if (existing != null && !existing.getId().equals(station.getId())) {
            throw new BusinessException("站点名称已存在");
        }
        station.setName(station.getName().trim());
        return stationRepository.save(station);
    }

    @Transactional
    public void deleteStation(Long id) {
        if (trainRepository.existsByFromStationIdOrToStationId(id, id)) {
            throw new BusinessException("该站点仍被车次引用，无法删除");
        }
        stationRepository.deleteById(id);
    }

    // ---------- 车次 ----------

    public List<Train> listTrains() {
        return trainRepository.findAll();
    }

    @Transactional
    public Train saveTrain(Train train) {
        if (train.getTrainNumber() == null || train.getTrainNumber().isBlank()) {
            throw new BusinessException("车次号不能为空");
        }
        Train existing = trainRepository.findByTrainNumber(train.getTrainNumber().trim());
        if (existing != null && !existing.getId().equals(train.getId())) {
            throw new BusinessException("车次号已存在");
        }
        if (!stationRepository.existsById(train.getFromStationId())
                || !stationRepository.existsById(train.getToStationId())) {
            throw new BusinessException("起点或终点站不存在");
        }
        train.setTrainNumber(train.getTrainNumber().trim());
        return trainRepository.save(train);
    }

    @Transactional
    public void deleteTrain(Long id) {
        if (orderRepository.existsByTrainId(id)) {
            throw new BusinessException("该车次存在订单，无法删除");
        }
        ticketRepository.findByTrainId(id).forEach(ticketRepository::delete);
        trainRepository.deleteById(id);
    }

    // ---------- 余票 ----------

    public List<Ticket> listTickets() {
        return ticketRepository.findAll();
    }

    @Transactional
    public Ticket adjustSeat(Long trainId, LocalDate trainDate, String seatType, int delta) {
        if (delta == 0) {
            throw new BusinessException("调整数量不能为 0");
        }
        SeatType type;
        try {
            type = SeatType.fromLabel(seatType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的座位类型: " + seatType);
        }
        Ticket ticket = ticketRepository.findByTrainIdAndTrainDate(trainId, trainDate)
                .orElseThrow(() -> new BusinessException("该车次该日期没有余票记录"));
        int current = switch (type) {
            case BUSINESS_SEAT -> ticket.getBusinessSeatCount();
            case FIRST_SEAT -> ticket.getFirstSeatCount();
            case SECOND_SEAT -> ticket.getSecondSeatCount();
            case SLEEPER -> ticket.getSleeperCount();
        };
        int next = current + delta;
        if (next < 0) {
            throw new BusinessException("余票不足，不能扣减为负数");
        }
        switch (type) {
            case BUSINESS_SEAT -> ticket.setBusinessSeatCount(next);
            case FIRST_SEAT -> ticket.setFirstSeatCount(next);
            case SECOND_SEAT -> ticket.setSecondSeatCount(next);
            case SLEEPER -> ticket.setSleeperCount(next);
        }
        Ticket saved = ticketRepository.save(ticket);
        ticketCacheVersion.invalidate();
        return saved;
    }
}
