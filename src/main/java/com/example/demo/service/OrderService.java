package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.BuyTicketRequest;
import com.example.demo.dto.OrderEventPayload;
import com.example.demo.entity.*;
import com.example.demo.mq.OrderEventDispatcher;
import com.example.demo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OrderRepository orderRepository;
    private final TrainRepository trainRepository;
    private final TicketRepository ticketRepository;
    private final StationRepository stationRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final MessageService messageService;
    private final TicketCacheVersion ticketCacheVersion;
    private final OrderEventDispatcher eventDispatcher;
    private final TransactionTemplate transactionTemplate;

    public OrderService(OrderRepository orderRepository,
                         TrainRepository trainRepository,
                         TicketRepository ticketRepository,
                         StationRepository stationRepository,
                         OrderOutboxRepository orderOutboxRepository,
                         MessageService messageService,
                         TicketCacheVersion ticketCacheVersion,
                         OrderEventDispatcher eventDispatcher,
                         PlatformTransactionManager transactionManager) {
        this.orderRepository = orderRepository;
        this.trainRepository = trainRepository;
        this.ticketRepository = ticketRepository;
        this.stationRepository = stationRepository;
        this.orderOutboxRepository = orderOutboxRepository;
        this.messageService = messageService;
        this.ticketCacheVersion = ticketCacheVersion;
        this.eventDispatcher = eventDispatcher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 购票入口（异步化）：校验参数后，在同一个事务里创建 PENDING 订单 + 事务发件箱事件，
     * 提交后由 worker 异步完成扣票、选座、状态流转与通知。下单接口不再直接竞争座位行锁，
     * 从而削峰并保证"订单创建"与"事件记录"原子一致。
     */
    public Order buyTicket(Long userId, BuyTicketRequest req) {
        Train train = trainRepository.findById(req.getTrainId())
                .orElseThrow(() -> new BusinessException("车次不存在"));
        SeatType seatType;
        try {
            seatType = SeatType.fromLabel(req.getSeatType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的座位类型: " + req.getSeatType());
        }
        LocalDate.parse(req.getTrainDate());

        return transactionTemplate.execute(status -> {
            Station fromStation = stationRepository.findById(train.getFromStationId()).orElse(null);
            Station toStation = stationRepository.findById(train.getToStationId()).orElse(null);

            Order order = new Order();
            order.setOrderNo("ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
            order.setUserId(userId);
            order.setTrainId(train.getId());
            order.setTrainNumber(train.getTrainNumber());
            order.setFromStation(fromStation != null ? fromStation.getName() : "");
            order.setToStation(toStation != null ? toStation.getName() : "");
            order.setTrainDate(req.getTrainDate());
            order.setSeatType(seatType.getLabel());
            order.setPassengerName(req.getPassengerName());
            order.setPassengerIdCard(req.getPassengerIdCard());
            order.setPrice(getPriceBySeatType(train, seatType));
            order.setStatus(OrderStatus.PENDING.name());
            order.setCreateTime(LocalDateTime.now());
            // saveAndFlush：让订单号唯一约束在事务内立即校验。
            Order saved = orderRepository.saveAndFlush(order);

            String eventId = UUID.randomUUID().toString();
            OrderOutboxEvent event = new OrderOutboxEvent();
            event.setEventId(eventId);
            event.setOrderId(saved.getId());
            event.setEventType(OrderEventTypes.ORDER_CREATED);
            event.setPayload(eventPayload(eventId, saved));
            event.setNextRetryAt(LocalDateTime.now());
            orderOutboxRepository.save(event);
            log.info("用户 {} 下单 {} 已创建（PENDING），事件 {} 待异步履约", userId, saved.getOrderNo(), eventId);

            // 事务提交后才投递：避免订单回滚后 MQ 里残留幽灵事件。
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventDispatcher.dispatch(eventId);
                    }
                });
            } else {
                eventDispatcher.dispatch(eventId);
            }
            return saved;
        });
    }

    private String eventPayload(String eventId, Order order) {
        OrderEventPayload payload = new OrderEventPayload();
        payload.setEventId(eventId);
        payload.setOrderId(order.getId());
        payload.setUserId(order.getUserId());
        payload.setTrainId(order.getTrainId());
        payload.setTrainDate(order.getTrainDate());
        payload.setSeatType(order.getSeatType());
        payload.setPassengerName(order.getPassengerName());
        payload.setPassengerIdCard(order.getPassengerIdCard());
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BusinessException("订单事件序列化失败");
        }
    }

    private Double getPriceBySeatType(Train train, SeatType seatType) {
        return switch (seatType) {
            case BUSINESS_SEAT -> train.getBusinessSeatPrice();
            case FIRST_SEAT -> train.getFirstSeatPrice();
            case SECOND_SEAT -> train.getSecondSeatPrice();
            case SLEEPER -> train.getSleeperPrice();
        };
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
        // 让出唯一键，使座位可被重新分配；保留原号作为审计痕迹。
        if (order.getSeatNo() != null && !order.getSeatNo().contains("#")) {
            order.setSeatNo(order.getSeatNo() + "#F" + order.getId());
        }
        Order saved = orderRepository.save(order);
        messageService.sendSystemMessage(userId,
                String.format("退票成功！%s %s → %s 已退票，退款 %s 元将原路返回",
                        saved.getTrainNumber(), saved.getFromStation(), saved.getToStation(), saved.getPrice()),
                saved.getId());
        invalidateTicketCache();
        return saved;
    }

    /**
     * 事务提交后再递增缓存版本，避免其他线程在提交前把旧余票重新写入缓存；
     * 无事务（如单元测试直接调用）时立即失效。
     */
    private void invalidateTicketCache() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ticketCacheVersion.invalidate();
                }
            });
        } else {
            ticketCacheVersion.invalidate();
        }
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
