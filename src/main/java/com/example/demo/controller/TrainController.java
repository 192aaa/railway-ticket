package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.Station;
import com.example.demo.entity.TrainStation;
import com.example.demo.repository.StationRepository;
import com.example.demo.repository.TrainStationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "车次接口")
@RestController
@RequestMapping("/api/train")
public class TrainController {

    private final TrainStationRepository trainStationRepository;
    private final StationRepository stationRepository;

    public TrainController(TrainStationRepository trainStationRepository,
                           StationRepository stationRepository) {
        this.trainStationRepository = trainStationRepository;
        this.stationRepository = stationRepository;
    }

    @Operation(summary = "车次经停站")
    @GetMapping("/{trainId}/stops")
    public Result<List<Map<String, Object>>> stops(@PathVariable Long trainId) {
        List<TrainStation> tsList = trainStationRepository.findByTrainIdOrderBySort(trainId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainStation ts : tsList) {
            Map<String, Object> item = new HashMap<>();
            item.put("sort", ts.getSort());
            item.put("arriveTime", ts.getArriveTime());
            item.put("departTime", ts.getDepartTime());
            stationRepository.findById(ts.getStationId()).ifPresent(s -> {
                item.put("stationId", s.getId());
                item.put("stationName", s.getName());
                item.put("city", s.getCity());
            });
            result.add(item);
        }
        return Result.success(result);
    }
}