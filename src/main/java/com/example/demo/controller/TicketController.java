package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.QueryTicketRequest;
import com.example.demo.dto.TrainTicketVO;
import com.example.demo.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "票务查询接口")
@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "查询余票")
    @GetMapping("/query")
    public Result<List<TrainTicketVO>> query(@Valid QueryTicketRequest req) {
        return Result.success(ticketService.queryTickets(req.getFromStation(), req.getToStation(), req.getDate()));
    }
}