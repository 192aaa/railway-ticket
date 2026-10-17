package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.BuyTicketRequest;
import com.example.demo.entity.Order;
import com.example.demo.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订单接口")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "购票下单")
    @PostMapping("/buy")
    public Result<Order> buy(@RequestAttribute("userId") Long userId,
                             @Valid @RequestBody BuyTicketRequest req) {
        return Result.success(orderService.buyTicket(userId, req));
    }

    @Operation(summary = "我的订单")
    @GetMapping("/my")
    public Result<List<Order>> myOrders(@RequestAttribute("userId") Long userId) {
        return Result.success(orderService.getMyOrders(userId));
    }

    @Operation(summary = "退票")
    @PostMapping("/refund/{orderId}")
    public Result<Order> refund(@RequestAttribute("userId") Long userId,
                                @PathVariable Long orderId) {
        return Result.success(orderService.refundTicket(userId, orderId));
    }
}