package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStateMachine;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.SeatType;
import com.example.demo.entity.Train;
import com.example.demo.dto.OrderEventPayload;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.TrainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单履约：异步 worker 消费事件时调用，负责扣票、选座、状态流转与通知。
 * 座位唯一约束作为并发兜底，冲突时整单重试（含余票回滚）。
 */
@Service
public class OrderFulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfillmentService.class);
    private static final int MAX_SEAT_RETRIES = 8;

    private final OrderRepository orderRepository;
    private final TrainRepository trainRepository;
    private final TicketRepository ticketRepository;
    private final MessageService messageService;
    private final TicketCacheVersion ticketCacheVersion;
    private final TransactionTemplate transactionTemplate;

    public OrderFulfillmentService(OrderRepository orderRepository,
                                   TrainRepository trainRepository,
                                   TicketRepository ticketRepository,
                                   MessageService messageService,
                                   TicketCacheVersion ticketCacheVersion,
                                   PlatformTransactionManager transactionManager) {
        this.orderRepository = orderRepository;
        this.trainRepository = trainRepository;
        this.ticketRepository = ticketRepository;
        this.messageService = messageService;
        this.ticketCacheVersion = ticketCacheVersion;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** 履约一张 PENDING 订单；成功后返回状态为 PAID 的订单。 */
    public Order fulfill(OrderEventPayload payload) {
        for (int attempt = 1; attempt <= MAX_SEAT_RETRIES; attempt++) {
            try {
                return transactionTemplate.execute(status -> fulfillOnce(payload));
            } catch (DataIntegrityViolationException e) {
                if (attempt >= MAX_SEAT_RETRIES) {
                    log.warn("订单 {} 座位冲突，重试 {} 次后仍失败", payload.getOrderId(), attempt);
                    throw new BusinessException("座位紧张，请稍后重试");
                }
                log.debug("订单 {} 座位冲突，第 {} 次重试", payload.getOrderId(), attempt);
            }
        }
        throw new BusinessException("座位紧张，请稍后重试");
    }

    private Order fulfillOnce(OrderEventPayload payload) {
        Order order = orderRepository.findById(payload.getOrderId())
                .orElseThrow(() -> new BusinessException("订单不存在"));

        // 幂等：只处理 PENDING 订单，避免事件重投导致重复扣票。
        OrderStatus current = OrderStatus.from(order.getStatus());
        if (current != OrderStatus.PENDING) {
            log.info("订单 {} 当前状态 {}，跳过重复履约", order.getId(), order.getStatus());
            return order;
        }

        Train train = trainRepository.findById(payload.getTrainId())
                .orElseThrow(() -> new BusinessException("车次不存在"));
        LocalDate date = LocalDate.parse(payload.getTrainDate());
        SeatType seatType;
        try {
            seatType = SeatType.fromLabel(payload.getSeatType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的座位类型: " + payload.getSeatType());
        }

        int affected = switch (seatType) {
            case BUSINESS_SEAT -> ticketRepository.deductBusinessSeat(train.getId(), date);
            case FIRST_SEAT -> ticketRepository.deductFirstSeat(train.getId(), date);
            case SECOND_SEAT -> ticketRepository.deductSecondSeat(train.getId(), date);
            case SLEEPER -> ticketRepository.deductSleeper(train.getId(), date);
        };
        if (affected == 0) {
            throw new BusinessException("座位类型 " + seatType.getLabel() + " 余票不足");
        }

        String seatNo = generateSeatNo(train.getId(), payload.getTrainDate(), seatType, seatType.getLabel());
        order.setSeatNo(seatNo);
        order.setStatus(OrderStateMachine.transition(OrderStatus.PENDING, OrderStatus.PAID).name());
        order.setPayTime(LocalDateTime.now());
        // saveAndFlush：让唯一约束冲突在事务内立即抛出，便于回滚后重试。
        Order saved = orderRepository.saveAndFlush(order);

        messageService.sendSystemMessage(payload.getUserId(),
                String.format("购票成功！%s %s → %s，座位 %s 号",
                        saved.getTrainNumber(), saved.getFromStation(), saved.getToStation(), saved.getSeatNo()),
                saved.getId());
        ticketCacheVersion.invalidate();
        return saved;
    }

    /** 生成符合真实车厢布局且未被占用的座位号。 */
    private String generateSeatNo(Long trainId, String trainDate, SeatType seatType, String seatLabel) {
        List<Order> soldOrders = orderRepository.findByTrainIdAndTrainDate(trainId, trainDate);
        Set<String> usedSeats = soldOrders.stream()
                .filter(o -> seatLabel.equals(o.getSeatType())
                        && OrderStatus.REFUNDED != OrderStatus.from(o.getStatus())
                        && o.getSeatNo() != null)
                .map(Order::getSeatNo)
                .collect(Collectors.toSet());

        List<String> candidates = buildSeatCandidates(seatType);
        Random random = new Random();
        int start = random.nextInt(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            String seatNo = candidates.get((start + i) % candidates.size());
            if (!usedSeats.contains(seatNo)) {
                return seatNo;
            }
        }
        throw new BusinessException("该车次座位已分配完毕");
    }

    private List<String> buildSeatCandidates(SeatType seatType) {
        List<String> seats = new ArrayList<>();
        switch (seatType) {
            case BUSINESS_SEAT -> addSeatRows(seats, 4, "A", "C", "F");
            case FIRST_SEAT -> addSeatRows(seats, 13, "A", "C", "D", "F");
            case SECOND_SEAT -> addSeatRows(seats, 20, "A", "B", "C", "D", "F");
            case SLEEPER -> {
                for (int row = 1; row <= 27; row++) {
                    for (String berth : new String[]{"上", "中", "下"}) {
                        seats.add(row + berth);
                    }
                }
            }
        }
        return seats;
    }

    private void addSeatRows(List<String> seats, int rows, String... cols) {
        for (int row = 1; row <= rows; row++) {
            for (String col : cols) {
                seats.add(row + col);
            }
        }
    }
}
