package com.example.demo.service;

import com.example.demo.entity.TrainStation;
import com.example.demo.repository.StationRepository;
import com.example.demo.repository.TrainStationRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrainService {

    private final TrainStationRepository trainStationRepository;
    private final StationRepository stationRepository;

    public TrainService(TrainStationRepository trainStationRepository,
                        StationRepository stationRepository) {
        this.trainStationRepository = trainStationRepository;
        this.stationRepository = stationRepository;
    }

    @Cacheable(value = "trains", key = "#trainId")
    public List<Map<String, Object>> stops(Long trainId) {
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
        return result;
    }
}
