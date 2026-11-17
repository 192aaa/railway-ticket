package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.Station;
import com.example.demo.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "站点接口")
@RestController
@RequestMapping("/api/station")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @Operation(summary = "站点列表")
    @GetMapping("/list")
    public Result<List<Station>> list() {
        return Result.success(stationService.listAll());
    }

    @Operation(summary = "搜索站点")
    @GetMapping("/search")
    public Result<List<Station>> search(@RequestParam(required = false) String keyword) {
        return Result.success(stationService.search(keyword));
    }
}
