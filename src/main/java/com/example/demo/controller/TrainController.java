package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.service.TrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "车次接口")
@RestController
@RequestMapping("/api/train")
public class TrainController {

    private final TrainService trainService;

    public TrainController(TrainService trainService) {
        this.trainService = trainService;
    }

    @Operation(summary = "车次经停站")
    @GetMapping("/{trainId}/stops")
    public Result<List<Map<String, Object>>> stops(@PathVariable Long trainId) {
        return Result.success(trainService.stops(trainId));
    }
}
