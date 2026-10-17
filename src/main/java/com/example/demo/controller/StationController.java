package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.Station;
import com.example.demo.repository.StationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "站点接口")
@RestController
@RequestMapping("/api/station")
public class StationController {

    private final StationRepository stationRepository;

    public StationController(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Operation(summary = "站点列表")
    @GetMapping("/list")
    public Result<List<Station>> list() {
        return Result.success(stationRepository.findAll());
    }

    @Operation(summary = "搜索站点")
    @GetMapping("/search")
    public Result<List<Station>> search(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return Result.success(stationRepository.findAll());
        }
        return Result.success(stationRepository.findByNameContaining(keyword));
    }
}