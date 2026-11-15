package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.OrderStatus;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.TrainRepository;
import com.example.demo.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Tag(name = "数据统计接口")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final OrderRepository orderRepository;
    private final TrainRepository trainRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public DashboardController(OrderRepository orderRepository,
                               TrainRepository trainRepository,
                               TicketRepository ticketRepository,
                               UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.trainRepository = trainRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "首页统计概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        String today = LocalDate.now().toString();

        data.put("totalOrders", orderRepository.count());
        data.put("todayOrders", orderRepository.countTodayPaid(today));
        data.put("todayRevenue", Optional.ofNullable(orderRepository.sumTodayRevenue(today)).orElse(0.0));
        data.put("totalTrains", trainRepository.count());
        data.put("totalUsers", userRepository.count());

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            statusCounts.put(s.name(), orderRepository.countByStatus(s.name()));
        }
        data.put("statusCounts", statusCounts);

        return Result.success(data);
    }

    @Operation(summary = "热门路线 Top10")
    @GetMapping("/top-routes")
    public Result<List<Map<String, Object>>> topRoutes() {
        List<Object[]> raw = orderRepository.findTopRoutes();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(raw.size(), 10); i++) {
            Object[] row = raw.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fromStation", row[0]);
            item.put("toStation", row[1]);
            item.put("count", row[2]);
            result.add(item);
        }
        return Result.success(result);
    }

    @Operation(summary = "近7日购票趋势")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend() {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String dateStr = d.toString();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateStr.substring(5));
            item.put("orders", orderRepository.countTodayPaid(dateStr));
            Double rev = orderRepository.sumTodayRevenue(dateStr);
            item.put("revenue", Optional.ofNullable(rev).orElse(0.0));
            result.add(item);
        }
        return Result.success(result);
    }
}