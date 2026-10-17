package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.BuyTicketRequest;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final TrainRepository trainRepository;
    private final TicketRepository ticketRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    public OrderService(OrderRepository orderRepository,
                         TrainRepository trainRepository,
                         TicketRepository ticketRepository,
                         StationRepository stationRepository,
                         UserRepository userRepository,
                         MessageService messageService) {
        this.orderRepository = orderRepository;
        this.trainRepository = trainRepository;
        this.ticketRepository = ticketRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    @Transactional
    public Order buyTicket(Long userId, BuyTicketRequest req) {
        Train train = trainRepository.findById(req.getTrainId())
                .orElseThrow(() -> new BusinessException("车次不存在"));

        LocalDate date = LocalDate.parse(req.getTrainDate());

        SeatType seatType;
        try {
            seatType = SeatType.fromLabel(req.getSeatType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的座位类型: " + req.getSeatType());
        }

        int affected = deductSeatAtomic(train.getId(), date, seatType);
        if (affected == 0) {
            throw new BusinessException("座位类型 " + seatType.getLabel() + " 余票不足");
        }

        Station fromStation = stationRepository.findById(train.getFromStationId()).orElse(null);
        Station toStation = stationRepository.findById(train.getToStationId()).orElse(null);

        Double price = getPriceBySeatType(train, seatType);
        String seatNo = generateSeatNo(train.getId(), req.getTrainDate(), seatType.getLabel());

        Order order = new Order();
        order.setOrderNo("ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        order.setUserId(userId);
        order.setTrainId(train.getId());
        order.setTrainNumber(train.getTrainNumber());
        order.setFromStation(fromStation != null ? fromStation.getName() : "");
        order.setToStation(toStation != null ? toStation.getName() : "");
        order.setTrainDate(req.getTrainDate());
        order.setSeatType(seatType.getLabel());
        order.setSeatNo(seatNo);
        order.setPassengerName(req.getPassengerName());
        order.setPassengerIdCard(req.getPassengerIdCard());
        order.setPrice(price);
        order.setStatus(OrderStatus.PAID.name());
        order.setCreateTime(LocalDateTime.now());
        order.setPayTime(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        messageService.sendSystemMessage(userId,
                String.format("购票成功！%s %s → %s，座位 %s 号",
                        saved.getTrainNumber(), saved.getFromStation(), saved.getToStation(), saved.getSeatNo()),
                saved.getId());
        return saved;
    }

    private int deductSeatAtomic(Long trainId, LocalDate date, SeatType seatType) {
        return switch (seatType) {
            case BUSINESS_SEAT -> ticketRepository.deductBusinessSeat(trainId, date);
            case FIRST_SEAT -> ticketRepository.deductFirstSeat(trainId, date);
            case SECOND_SEAT -> ticketRepository.deductSecondSeat(trainId, date);
            case SLEEPER -> ticketRepository.deductSleeper(trainId, date);
        };
    }

    private Double getPriceBySeatType(Train train, SeatType seatType) {
        return switch (seatType) {
            case BUSINESS_SEAT -> train.getBusinessSeatPrice();
            case FIRST_SEAT -> train.getFirstSeatPrice();
            case SECOND_SEAT -> train.getSecondSeatPrice();
            case SLEEPER -> train.getSleeperPrice();
        };
    }

    private String generateSeatNo(Long trainId, String trainDate, String seatType) {
        List<Order> soldOrders = orderRepository.findByTrainIdAndTrainDate(trainId, trainDate);
        Set<String> usedSeats = soldOrders.stream()
                .filter(o -> seatType.equals(o.getSeatType())
                        && OrderStatus.REFUNDED != OrderStatus.from(o.getStatus()))
                .map(Order::getSeatNo)
                .collect(Collectors.toSet());

        String[] cols = {"A", "B", "C", "D", "F"};
        Random random = new Random();
        for (int attempt = 0; attempt < 200; attempt++) {
            int row = random.nextInt(15) + 1;
            String col = cols[random.nextInt(cols.length)];
            String seatNo = row + col;
            if (!usedSeats.contains(seatNo)) {
                return seatNo;
            }
        }
        throw new BusinessException("该车次座位已分配完毕");
    }

    public List<Order> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    @Transactional
    public Order refundTicket(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        OrderStatus st = OrderStatus.from(order.getStatus());
        if (st == null) {
            throw new BusinessException("订单状态异常");
        }
        if (st == OrderStatus.REFUNDED) {
            throw new BusinessException("该订单已退票");
        }
        if (st != OrderStatus.PAID && st != OrderStatus.COMPLETED) {
            throw new BusinessException("当前状态不可退票");
        }

        Train train = trainRepository.findById(order.getTrainId())
                .orElseThrow(() -> new BusinessException("车次不存在"));

        LocalDateTime departureTime = parseDepartureTime(order.getTrainDate(), train.getStartTime());
        if (LocalDateTime.now().isAfter(departureTime)) {
            throw new BusinessException("列车已发车，无法退票");
        }

        SeatType seatType = null;
        try {
            seatType = SeatType.fromLabel(order.getSeatType());
        } catch (IllegalArgumentException ignored) {}

        if (seatType != null) {
            LocalDate date = LocalDate.parse(order.getTrainDate());
            restoreSeatAtomic(order.getTrainId(), date, seatType);
        }

        order.setStatus(OrderStatus.REFUNDED.name());
        Order saved = orderRepository.save(order);
        messageService.sendSystemMessage(userId,
                String.format("退票成功！%s %s → %s 已退票，退款 %s 元将原路返回",
                        saved.getTrainNumber(), saved.getFromStation(), saved.getToStation(), saved.getPrice()),
                saved.getId());
        return saved;
    }

    private LocalDateTime parseDepartureTime(String trainDate, String startTime) {
        try {
            LocalDate d = LocalDate.parse(trainDate);
            LocalTime t = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
            return LocalDateTime.of(d, t);
        } catch (Exception e) {
            return LocalDateTime.of(LocalDate.parse(trainDate), LocalTime.of(23, 59));
        }
    }

    private void restoreSeatAtomic(Long trainId, LocalDate date, SeatType seatType) {
        switch (seatType) {
            case BUSINESS_SEAT -> ticketRepository.restoreBusinessSeat(trainId, date);
            case FIRST_SEAT -> ticketRepository.restoreFirstSeat(trainId, date);
            case SECOND_SEAT -> ticketRepository.restoreSecondSeat(trainId, date);
            case SLEEPER -> ticketRepository.restoreSleeper(trainId, date);
        }
    }
}