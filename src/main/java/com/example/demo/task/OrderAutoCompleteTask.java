package com.example.demo.task;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.Train;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.TrainRepository;
import com.example.demo.service.MessageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderAutoCompleteTask {

    private final OrderRepository orderRepository;
    private final TrainRepository trainRepository;
    private final MessageService messageService;

    public OrderAutoCompleteTask(OrderRepository orderRepository,
                                 TrainRepository trainRepository,
                                 MessageService messageService) {
        this.orderRepository = orderRepository;
        this.trainRepository = trainRepository;
        this.messageService = messageService;
    }

    @Scheduled(fixedDelay = 60000)
    public void autoCompleteDepartedOrders() {
        List<Order> paidOrders = orderRepository.findByStatus(OrderStatus.PAID.name());
        LocalDateTime now = LocalDateTime.now();
        List<Order> completedOrders = new ArrayList<>();
        // 车次一次加载，避免每单一次 findById 查询
        Map<Long, Train> trainMap = trainRepository.findAll().stream()
                .collect(Collectors.toMap(Train::getId, Function.identity()));

        for (Order order : paidOrders) {
            try {
                Train train = trainMap.get(order.getTrainId());
                if (train == null || train.getStartTime() == null) continue;

                LocalDateTime departureTime = parseDepartureTime(order.getTrainDate(), train.getStartTime());
                if (now.isAfter(departureTime.plusMinutes(10))) {
                    order.setStatus(OrderStatus.COMPLETED.name());
                    completedOrders.add(order);
                }
            } catch (Exception ignored) {}
        }

        if (completedOrders.isEmpty()) return;

        // 一次性批量更新，避免逐单 save 造成的多次事务与网络往返
        orderRepository.saveAll(completedOrders);
        for (Order order : completedOrders) {
            messageService.sendSystemMessage(order.getUserId(),
                    String.format("乘车完成！%s %s → %s，感谢您乘坐",
                            order.getTrainNumber(), order.getFromStation(), order.getToStation()),
                    order.getId());
        }
    }

    private LocalDateTime parseDepartureTime(String trainDate, String startTime) {
        try {
            return LocalDateTime.parse(trainDate + "T" + startTime + ":00");
        } catch (Exception e) {
            try {
                return LocalDateTime.of(java.time.LocalDate.parse(trainDate),
                        LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm")));
            } catch (Exception e2) {
                return LocalDateTime.of(java.time.LocalDate.parse(trainDate), LocalTime.of(23, 59));
            }
        }
    }
}