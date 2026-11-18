package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.Order;
import com.example.demo.entity.Station;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.Train;
import com.example.demo.entity.User;
import com.example.demo.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 管理端接口：仅 ADMIN 角色可访问，由 JwtAuthFilter 统一拦截。 */
@Tag(name = "管理端接口")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<List<User>> listUsers() {
        return Result.success(adminService.listUsers());
    }

    @Operation(summary = "修改用户角色")
    @PutMapping("/users/{id}/role")
    public Result<User> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(adminService.updateUserRole(id, body.get("role")));
    }

    @Operation(summary = "订单列表（可按状态过滤）")
    @GetMapping("/orders")
    public Result<List<Order>> listOrders(@RequestParam(required = false) String status) {
        return Result.success(adminService.listOrders(status));
    }

    @Operation(summary = "站点列表")
    @GetMapping("/stations")
    public Result<List<Station>> listStations() {
        return Result.success(adminService.listStations());
    }

    @Operation(summary = "新增站点")
    @PostMapping("/stations")
    public Result<Station> createStation(@RequestBody Station station) {
        station.setId(null);
        return Result.success(adminService.saveStation(station));
    }

    @Operation(summary = "更新站点")
    @PutMapping("/stations/{id}")
    public Result<Station> updateStation(@PathVariable Long id, @RequestBody Station station) {
        station.setId(id);
        return Result.success(adminService.saveStation(station));
    }

    @Operation(summary = "删除站点")
    @DeleteMapping("/stations/{id}")
    public Result<Void> deleteStation(@PathVariable Long id) {
        adminService.deleteStation(id);
        return Result.success(null);
    }

    @Operation(summary = "车次列表")
    @GetMapping("/trains")
    public Result<List<Train>> listTrains() {
        return Result.success(adminService.listTrains());
    }

    @Operation(summary = "新增车次")
    @PostMapping("/trains")
    public Result<Train> createTrain(@RequestBody Train train) {
        train.setId(null);
        return Result.success(adminService.saveTrain(train));
    }

    @Operation(summary = "更新车次")
    @PutMapping("/trains/{id}")
    public Result<Train> updateTrain(@PathVariable Long id, @RequestBody Train train) {
        train.setId(id);
        return Result.success(adminService.saveTrain(train));
    }

    @Operation(summary = "删除车次")
    @DeleteMapping("/trains/{id}")
    public Result<Void> deleteTrain(@PathVariable Long id) {
        adminService.deleteTrain(id);
        return Result.success(null);
    }

    @Operation(summary = "余票列表")
    @GetMapping("/tickets")
    public Result<List<Ticket>> listTickets() {
        return Result.success(adminService.listTickets());
    }

    @Operation(summary = "调整余票（delta 可为负数）")
    @PostMapping("/tickets/adjust")
    public Result<Ticket> adjustTicket(@RequestBody Map<String, Object> body) {
        Long trainId = Long.valueOf(String.valueOf(body.get("trainId")));
        LocalDate date = LocalDate.parse(String.valueOf(body.get("trainDate")));
        String seatType = String.valueOf(body.get("seatType"));
        int delta = Integer.parseInt(String.valueOf(body.get("delta")));
        return Result.success(adminService.adjustSeat(trainId, date, seatType, delta));
    }
}
